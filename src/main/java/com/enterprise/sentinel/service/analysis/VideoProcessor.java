package com.enterprise.sentinel.service.analysis;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import com.enterprise.sentinel.client.ui.SentinelVideoView;
import com.enterprise.sentinel.domain.model.DetectionEvent;
import com.enterprise.sentinel.domain.repository.DetectionEventRepository;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Phase 2: PERF-01 Performance Gate & Throttling
 * 
 * Implements fail-proof AI throttling at ~2 FPS (500ms interval).
 * Uses a lightweight async executor for non-blocking inference.
 * 
 * Data Flow:
 * 1. Frame arrives from UI
 * 2. Gate: Is 500ms elapsed? If not, drop.
 * 3. If yes, spawn async task for inference
 * 4. Convert image → DJL → ONNX inference
 * 5. Save detections and update UI
 * 
 * Thread Safety: AtomicLong for CAS-based time tracking
 * Fail-Safe: Time checks, null guards, error isolation
 */
@Service
public class VideoProcessor {

    private static final Logger LOGGER = Logger.getLogger(VideoProcessor.class.getName());
    
    // PERF-01: 500ms throttle = 2 FPS max
    private static final long THROTTLE_INTERVAL_MS = 500L;
    
    private final ObjectDetectionService detectionService;
    private final FrameRateLimiter frameRateLimiter;
    private final AlertEngine alertEngine;
    private final DetectionEventRepository detectionEventRepository;
    private final ExecutorService inferenceExecutor;
    private SentinelVideoView videoView;
    
    // ATOMIC: Track last processing time (lock-free)
    private final AtomicLong lastProcessingTimeMs = new AtomicLong(0L);
    
    // METRICS: Track throttled and processed frames
    private final AtomicLong processedFrameCount = new AtomicLong(0L);
    private final AtomicLong throttledFrameCount = new AtomicLong(0L);
    private final AtomicLong inferenceErrorCount = new AtomicLong(0L);

    public VideoProcessor(ObjectDetectionService detectionService, 
                         FrameRateLimiter frameRateLimiter,
                         AlertEngine alertEngine,
                         DetectionEventRepository detectionEventRepository) {
        this.detectionService = detectionService;
        this.frameRateLimiter = frameRateLimiter;
        this.alertEngine = alertEngine;
        this.detectionEventRepository = detectionEventRepository;
        
        // Create a lightweight executor for inference tasks
        // Single thread to prevent concurrent inference (one inference at a time)
        this.inferenceExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sentinel-inference");
            thread.setDaemon(false);
            return thread;
        });
    }

    public void setVideoView(SentinelVideoView view) {
        this.videoView = view;
    }

    /**
     * Phase 2: PERF-01 Throttle Gate
     * 
     * Called whenever a new frame is rendered by VLC.
     * Implements fail-proof throttling: drop if < 500ms since last inference.
     * 
     * Guarantee: At most 1 inference every 500ms (2 FPS max).
     * 
     * @param fxImage Current video frame from UI pipeline
     */
    public void processFrame(WritableImage fxImage) {
        // FAIL-SAFE: Validate input
        if (fxImage == null) {
            LOGGER.warning("Received null frame, dropping");
            return;
        }

        if (videoView == null) {
            LOGGER.fine("VideoView not set, frame dropped");
            throttledFrameCount.incrementAndGet();
            return;
        }

        // THROTTLE GATE: Check elapsed time
        long now = System.currentTimeMillis();
        long lastRun = lastProcessingTimeMs.get();
        long delta = now - lastRun;

        // FAIL-SAFE: If throttle not elapsed, drop frame
        if (delta < THROTTLE_INTERVAL_MS) {
            throttledFrameCount.incrementAndGet();
            LOGGER.fine("Throttled: " + delta + " ms since last inference");
            return;
        }

        // ATOMIC UPDATE: Record this as the new processing time
        if (!lastProcessingTimeMs.compareAndSet(lastRun, now)) {
            LOGGER.fine("Race detected: another thread is processing");
            throttledFrameCount.incrementAndGet();
            return;
        }

        // ASYNC: Submit inference task to executor
        inferenceExecutor.submit(() -> {
            try {
                processFrameAsync(fxImage);
                processedFrameCount.incrementAndGet();
            } catch (Exception e) {
                LOGGER.severe("Inference thread error: " + e.getMessage());
                inferenceErrorCount.incrementAndGet();
                e.printStackTrace();
            }
        });

        // Non-blocking: method returns immediately
        LOGGER.fine("Submitted inference task");
    }

    /**
     * Asynchronous inference pipeline (runs in executor thread).
     * 
     * Steps:
     * 1. Convert JavaFX Image → DJL Image
     * 2. Run ONNX inference
     * 3. Filter low-confidence detections
     * 4. Persist to database
     * 5. Evaluate alert rules
     * 6. Update UI overlay
     * 
     * @param fxImage Frame to analyze
     * @throws Exception on inference errors
     */
    private void processFrameAsync(WritableImage fxImage) throws Exception {
        long startTime = System.currentTimeMillis();

        // 1. Convert JavaFX → BufferedImage → DJL
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);
        if (bufferedImage == null) {
            LOGGER.warning("Failed to convert FX image to BufferedImage");
            return;
        }

        Image djlImage = ImageFactory.getInstance().fromImage(bufferedImage);

        // 2. INFERENCE: Run YOLOv8 ONNX
        DetectedObjects detections = detectionService.detect(djlImage);

        long inferenceTime = System.currentTimeMillis() - startTime;
        LOGGER.info("Inference completed in " + inferenceTime + " ms");

        // 3. Process and save detections
        if (detections != null && !detections.items().isEmpty()) {
            processAndSaveDetections(detections);
        }

        // 4. Update UI overlay (on FX thread)
        if (videoView != null) {
            Platform.runLater(() -> {
                videoView.drawDetections(detections);
            });
        }
    }

    /**
     * Save high-confidence detections to database.
     * Evaluate against alert rules (geofences, event types).
     * 
     * @param detections Results from inference
     */
    private void processAndSaveDetections(DetectedObjects detections) {
        if (detections == null || detections.items().isEmpty()) {
            return;
        }

        long timestampMs = System.currentTimeMillis();

        // Iterate over detections
        detections.items().forEach(item -> {
            double confidence = 0.0;
            String className = "";
            String bbox = "";
            
            // Handle both possible item types from DJL
            if (item instanceof ai.djl.modality.Classifications.Classification) {
                ai.djl.modality.Classifications.Classification classification = 
                    (ai.djl.modality.Classifications.Classification) item;
                confidence = classification.getProbability();
                className = classification.getClassName();
            } else if (item instanceof ai.djl.modality.cv.output.DetectedObjects.DetectedObject) {
                ai.djl.modality.cv.output.DetectedObjects.DetectedObject detected = 
                    (ai.djl.modality.cv.output.DetectedObjects.DetectedObject) item;
                confidence = detected.getProbability();
                className = detected.getClassName();
                bbox = detected.getBoundingBox().toString();
            }
            
            // FAIL-SAFE: Only persist high-confidence detections (>50%)
            if (confidence > 0.5) {
                try {
                    DetectionEvent detectionEvent = DetectionEvent.builder()
                            .timestampMs(timestampMs)
                            .detectedClass(className)
                            .confidence(confidence)
                            .boundingBox(bbox)
                            .inferenceData(buildInferenceData(className, confidence))
                            .build();

                    DetectionEvent saved = detectionEventRepository.save(detectionEvent);

                    // SEC-01: Evaluate detection against geofence zones and trigger alerts
                    alertEngine.processDetection(saved);
                    
                    LOGGER.info("Saved detection: " + className + " (" + confidence + ")");
                } catch (Exception e) {
                    LOGGER.severe("Error persisting detection: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Build inference metadata for persistence.
     * 
     * @return Map with model info, class, confidence
     */
    private Map<String, Object> buildInferenceData(String className, double confidence) {
        Map<String, Object> data = new HashMap<>();
        data.put("class", className);
        data.put("confidence", confidence);
        data.put("model", "yolov8n");
        data.put("engine", "OnnxRuntime");
        return data;
    }

    // ===== Metrics & Observability =====

    public long getProcessedFrameCount() {
        return processedFrameCount.get();
    }

    public long getThrottledFrameCount() {
        return throttledFrameCount.get();
    }

    public long getInferenceErrorCount() {
        return inferenceErrorCount.get();
    }

    public double getActualInferenceFps() {
        return processedFrameCount.get() * 1000.0 / (System.currentTimeMillis() + 1);
    }

    public void logMetrics() {
        LOGGER.info(String.format(
            "VideoProcessor Metrics: processed=%d, throttled=%d, errors=%d, fps=%.2f",
            processedFrameCount.get(),
            throttledFrameCount.get(),
            inferenceErrorCount.get(),
            getActualInferenceFps()
        ));
    }

    public void shutdown() {
        inferenceExecutor.shutdown();
        LOGGER.info("VideoProcessor executor shutdown");
    }
}