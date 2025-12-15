package com.enterprise.sentinel.client.ui;

import com.enterprise.sentinel.client.video.JavaFxVideoSurface;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import java.util.function.Consumer;

public class SentinelVideoView extends StackPane {

    private final ImageView imageView;
    private final Canvas overlayCanvas; // NEW: Layer 2
    private final MediaPlayerFactory mediaPlayerFactory;
    private final EmbeddedMediaPlayer mediaPlayer;
    
    private Consumer<WritableImage> frameListener; // Hook for AI

    public SentinelVideoView() {
        // 1. Layer 1: Video Feed
        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
        this.imageView.fitWidthProperty().bind(this.widthProperty());
        this.imageView.fitHeightProperty().bind(this.heightProperty());
        
        // 2. Layer 2: AI Overlay
        this.overlayCanvas = new Canvas();
        this.overlayCanvas.widthProperty().bind(this.widthProperty());
        this.overlayCanvas.heightProperty().bind(this.heightProperty());
        this.overlayCanvas.setMouseTransparent(true); // Let clicks pass through

        // Stack them: Image bottom, Canvas top
        this.getChildren().addAll(imageView, overlayCanvas);

        // 3. Setup VLCJ
        this.mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
        this.mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

        // 4. Setup Video Surface with Hook
        JavaFxVideoSurface videoSurface = new JavaFxVideoSurface(image -> {
            // Update UI
            this.imageView.setImage(image);
            
            // Send frame to AI (if listener exists)
            if (this.frameListener != null) {
                this.frameListener.accept(image);
            }
        });
        
        this.mediaPlayer.videoSurface().set(videoSurface);
    }

    public void setOnFrameReady(Consumer<WritableImage> listener) {
        this.frameListener = listener;
    }

    /**
     * Draws bounding boxes on the overlay.
     * @param detections List of DJL DetectedObjects
     */
    public void drawDetections(ai.djl.modality.cv.output.DetectedObjects detections) {
        Platform.runLater(() -> {
            GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
            // Clear previous frame's boxes
            gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
            
            if (detections == null) return;

            gc.setLineWidth(3);
            
            // FIX: Iterate as generic Classification, then cast to DetectedObject
            for (ai.djl.modality.Classifications.Classification item : detections.items()) {
                
                // Safe cast to access getBoundingBox()
                if (item instanceof ai.djl.modality.cv.output.DetectedObjects.DetectedObject obj) {
                    
                    ai.djl.modality.cv.output.BoundingBox bbox = obj.getBoundingBox();
                    ai.djl.modality.cv.output.Rectangle rect = bbox.getBounds();
                    
                    // Map normalized coordinates (0..1) to actual screen size
                    double x = rect.getX() * overlayCanvas.getWidth();
                    double y = rect.getY() * overlayCanvas.getHeight();
                    double w = rect.getWidth() * overlayCanvas.getWidth();
                    double h = rect.getHeight() * overlayCanvas.getHeight();

                    // Draw Box
                    gc.setStroke(Color.RED);
                    gc.strokeRect(x, y, w, h);

                    // Draw Label
                    gc.setFill(Color.RED);
                    gc.fillText(obj.getClassName() + String.format(" %.2f", obj.getProbability()), x, y - 5);
                }
            }
        });
    }

    public void play(String mrl) {
        String[] options = {
            ":network-caching=300",
            ":rtsp-tcp",
            ":clock-jitter=0",
            ":clock-synchro=0"
        };
        mediaPlayer.media().play(mrl, options);
    }

    public void stop() {
        mediaPlayer.controls().stop();
    }

    public void cleanup() {
        mediaPlayer.controls().stop();
        mediaPlayer.release();
        mediaPlayerFactory.release();
    }
}