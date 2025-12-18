package com.enterprise.sentinel.service.analysis;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import com.enterprise.sentinel.client.ui.SentinelVideoView;
import com.enterprise.sentinel.domain.model.DetectionEvent;
import com.enterprise.sentinel.domain.repository.DetectionEventRepository;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class VideoProcessor {

    private final ObjectDetectionService detectionService;
    private final FrameRateLimiter frameRateLimiter;
    private final AlertEngine alertEngine;
    private final DetectionEventRepository detectionEventRepository;
    private SentinelVideoView videoView;
    
    // Prevent backlog: if busy processing frame 1, drop frame 2
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public VideoProcessor(ObjectDetectionService detectionService, 
                         FrameRateLimiter frameRateLimiter,
                         AlertEngine alertEngine,
                         DetectionEventRepository detectionEventRepository) {
        this.detectionService = detectionService;
        this.frameRateLimiter = frameRateLimiter;
        this.alertEngine = alertEngine;
        this.detectionEventRepository = detectionEventRepository;
    }

    public void setVideoView(SentinelVideoView view) {
        this.videoView = view;
    }

    /**
     * Called whenever a new frame is rendered by VLC.
     * We run inference asynchronously to avoid blocking the UI/Video thread.
     * 
     * Flow:
     * 1. Frame rate limiting (PERF-01: 2 FPS cap)
     * 2. Object detection via YOLOv8
     * 3. Save detections to database
     * 4. Evaluate alerts against geofence zones
     * 5. Update UI with detection annotations
     */
    @Async
    public void processFrame(WritableImage fxImage) {
        // Flow Control 1: Drop frames if AI is busy
        if (videoView == null || isProcessing.getAndSet(true)) {
            return;
        }

        // Flow Control 2: Drop frames exceeding target rate (PERF-01: 2 FPS cap)
        if (!frameRateLimiter.shouldProcessFrame()) {
            isProcessing.set(false);
            return;
        }

        try {
            // 1. Convert JavaFX Image to DJL Image
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);
            Image djlImage = ImageFactory.getInstance().fromImage(bufferedImage);

            // 2. Run Inference
            DetectedObjects detections = detectionService.detect(djlImage);

            // 3. Persist high-confidence detections to database
            processAndSaveDetections(detections);

            // 4. Update UI
            videoView.drawDetections(detections);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isProcessing.set(false);
        }
    }

    /**
     * Save detections with confidence > 0.5 to database.
     * For each detection, evaluate against alert rules (AlertEngine).
     */
    private void processAndSaveDetections(DetectedObjects detections) {
        if (detections == null || detections.items().isEmpty()) {
            return;
        }

        long timestampMs = System.currentTimeMillis();

        detections.items().forEach(item -> {
            double confidence = item.getProbability();
            
            // Only persist high-confidence detections (>50%)
            if (confidence > 0.5) {
                DetectionEvent detectionEvent = DetectionEvent.builder()
                        .timestampMs(timestampMs)
                        .detectedClass(item.getClassName())
                        .confidence(confidence)
                        .boundingBox(item.getBounds().toString())
                        .inferenceData(buildInferenceData(item))
                        .build();

                DetectionEvent saved = detectionEventRepository.save(detectionEvent);

                // Evaluate detection against geofence zones and trigger alerts if needed
                alertEngine.processDetection(saved);
            }
        });
    }

    /**
     * Build inference data map from detected object.
     */
    private Map<String, Object> buildInferenceData(DetectedObjects.Item item) {
        Map<String, Object> data = new HashMap<>();
        data.put("class", item.getClassName());
        data.put("confidence", item.getProbability());
        data.put("bbox", item.getBounds().toString());
        data.put("model", "yolov8n");
        return data;
    }
}