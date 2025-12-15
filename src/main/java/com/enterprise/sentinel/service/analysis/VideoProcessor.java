package com.enterprise.sentinel.service.analysis;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import com.enterprise.sentinel.client.ui.SentinelVideoView;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class VideoProcessor {

    private final ObjectDetectionService detectionService;
    private SentinelVideoView videoView;
    
    // Prevent backlog: if busy processing frame 1, drop frame 2
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public VideoProcessor(ObjectDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    public void setVideoView(SentinelVideoView view) {
        this.videoView = view;
    }

    /**
     * Called whenever a new frame is rendered by VLC.
     * We run inference asynchronously to avoid blocking the UI/Video thread.
     */
    @Async
    public void processFrame(WritableImage fxImage) {
        // Flow Control: Drop frames if AI is busy
        if (videoView == null || isProcessing.getAndSet(true)) {
            return;
        }

        try {
            // 1. Convert JavaFX Image to DJL Image
            // Note: Optimizable later, good enough for MVP
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);
            Image djlImage = ImageFactory.getInstance().fromImage(bufferedImage);

            // 2. Run Inference
            DetectedObjects detections = detectionService.detect(djlImage);

            // 3. Update UI
            videoView.drawDetections(detections);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isProcessing.set(false);
        }
    }
}