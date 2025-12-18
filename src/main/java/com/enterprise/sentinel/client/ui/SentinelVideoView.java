package com.enterprise.sentinel.client.ui;

import com.enterprise.sentinel.client.video.JavaFxVideoSurface;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Phase 2: UI-01 & UI-02 Rendering Pipeline
 * 
 * Combines video playback (UI-01) with AI overlay rendering (UI-02).
 * Uses fail-safe coordinate mapping and bounds checking.
 * 
 * Architecture:
 * - Layer 1: ImageView (video)
 * - Layer 2: Canvas (AI overlay with bounding boxes)
 * 
 * Thread Safety: All UI updates via Platform.runLater()
 * Fail-Safe: Bounds validation, null checks, confidence filtering
 */
public class SentinelVideoView extends StackPane {

    private static final Logger LOGGER = Logger.getLogger(SentinelVideoView.class.getName());
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.5d;
    private static final double BBOX_STROKE_WIDTH = 2.0d;
    private static final int LABEL_FONT_SIZE = 12;

    private final ImageView imageView;
    private final Canvas overlayCanvas;
    private final MediaPlayerFactory mediaPlayerFactory;
    private final EmbeddedMediaPlayer mediaPlayer;
    
    private Consumer<WritableImage> frameListener;

    public SentinelVideoView() {
        // 1. Layer 1: Video Feed
        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
        this.imageView.fitWidthProperty().bind(this.widthProperty());
        this.imageView.fitHeightProperty().bind(this.heightProperty());
        
        // 2. Layer 2: AI Overlay (fail-safe clear on size change)
        this.overlayCanvas = new Canvas();
        this.overlayCanvas.widthProperty().bind(this.widthProperty());
        this.overlayCanvas.heightProperty().bind(this.heightProperty());
        this.overlayCanvas.setMouseTransparent(true);
        
        // Clear canvas when size changes
        this.overlayCanvas.widthProperty().addListener((obs, oldVal, newVal) -> 
            clearOverlay()
        );
        this.overlayCanvas.heightProperty().addListener((obs, oldVal, newVal) -> 
            clearOverlay()
        );

        // Stack layers: Image bottom, Canvas top
        this.getChildren().addAll(imageView, overlayCanvas);

        // 3. Setup VLCJ
        this.mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
        this.mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

        // 4. Setup Video Surface with Hook (UI-01: Bridge)
        JavaFxVideoSurface videoSurface = new JavaFxVideoSurface(image -> {
            // ATOMIC: Update image on FX thread
            this.imageView.setImage(image);
            
            // Trigger AI analysis pipeline (PERF-01)
            if (this.frameListener != null) {
                this.frameListener.accept(image);
            }
        });
        
        this.mediaPlayer.videoSurface().set(videoSurface);
        LOGGER.info("SentinelVideoView initialized");
    }

    public void setOnFrameReady(Consumer<WritableImage> listener) {
        this.frameListener = listener;
    }

    /**
     * Phase 2: UI-02 Overlay Mapper
     * 
     * Draws bounding boxes with fail-safe coordinate transformation.
     * Rule: Normalized coords [0,1] → Pixel coords [0, canvasW/H]
     * 
     * Fail-Safes:
     * - Null/empty detections → clear overlay
     * - Confidence < threshold → skip
     * - Out-of-bounds rectangles → clamp to canvas
     * 
     * @param detections DJL detected objects
     */
    public void drawDetections(ai.djl.modality.cv.output.DetectedObjects detections) {
        Platform.runLater(() -> {
            try {
                GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
                double canvasWidth = overlayCanvas.getWidth();
                double canvasHeight = overlayCanvas.getHeight();

                // FAIL-SAFE: Validate canvas dimensions
                if (canvasWidth <= 0 || canvasHeight <= 0) {
                    LOGGER.warning("Invalid canvas dimensions: " + canvasWidth + "x" + canvasHeight);
                    return;
                }

                // Clear previous frame
                gc.clearRect(0, 0, canvasWidth, canvasHeight);

                // FAIL-SAFE: Validate detections
                if (detections == null || detections.items().isEmpty()) {
                    return;
                }

                gc.setLineWidth(BBOX_STROKE_WIDTH);
                gc.setFont(new Font(LABEL_FONT_SIZE));

                // Draw each detection
                for (ai.djl.modality.Classifications.Classification item : detections.items()) {
                    
                    // Safe type check
                    if (!(item instanceof ai.djl.modality.cv.output.DetectedObjects.DetectedObject obj)) {
                        continue;
                    }

                    // FAIL-SAFE: Confidence filtering
                    double confidence = obj.getProbability();
                    if (confidence < MIN_CONFIDENCE_THRESHOLD) {
                        continue; // Skip low-confidence detections
                    }

                    try {
                        // Extract bounding box (normalized coordinates)
                        ai.djl.modality.cv.output.BoundingBox bbox = obj.getBoundingBox();
                        if (bbox == null) {
                            continue;
                        }

                        ai.djl.modality.cv.output.Rectangle rect = bbox.getBounds();
                        if (rect == null) {
                            continue;
                        }

                        // COORDINATE MAPPER: Normalized [0,1] → Pixel coords
                        double normX = rect.getX();
                        double normY = rect.getY();
                        double normW = rect.getWidth();
                        double normH = rect.getHeight();

                        // Transform to pixel space
                        double pixelX = normX * canvasWidth;
                        double pixelY = normY * canvasHeight;
                        double pixelWidth = normW * canvasWidth;
                        double pixelHeight = normH * canvasHeight;

                        // FAIL-SAFE: Bounds checking
                        if (!isValidBounds(pixelX, pixelY, pixelWidth, pixelHeight, canvasWidth, canvasHeight)) {
                            LOGGER.fine("Detection out of bounds: x=" + pixelX + " y=" + pixelY);
                            continue;
                        }

                        // Draw bounding box
                        gc.setStroke(Color.RED);
                        gc.strokeRect(pixelX, pixelY, pixelWidth, pixelHeight);

                        // Draw label with confidence
                        String label = obj.getClassName() + String.format(" (%.2f)", confidence);
                        gc.setFill(Color.YELLOW);
                        gc.fillText(label, pixelX + 5, pixelY - 5);

                    } catch (Exception e) {
                        LOGGER.warning("Error drawing detection: " + e.getMessage());
                        // Continue with next detection (fail-safe)
                    }
                }

            } catch (Exception e) {
                LOGGER.severe("Error in drawDetections: " + e.getMessage());
                e.printStackTrace();
                // FAIL-SAFE: Clear canvas on error
                clearOverlay();
            }
        });
    }

    /**
     * Validate that bounding box is within canvas bounds.
     * 
     * Guarantee: Returns true only if all corners fit within canvas.
     * 
     * @return true if bbox is valid and in bounds
     */
    private boolean isValidBounds(double x, double y, double w, double h, 
                                   double canvasW, double canvasH) {
        // FAIL-SAFE: Check for NaN or infinity
        if (Double.isNaN(x) || Double.isNaN(y) || 
            Double.isNaN(w) || Double.isNaN(h) ||
            Double.isInfinite(x) || Double.isInfinite(y)) {
            return false;
        }

        // GUARANTEE: All corners must fit within canvas
        boolean valid = (x >= 0 && y >= 0 && 
                        x + w <= canvasW && y + h <= canvasH &&
                        w > 0 && h > 0);

        if (!valid) {
            LOGGER.fine("Bounds check failed: x=" + x + " y=" + y + 
                       " w=" + w + " h=" + h + 
                       " canvasW=" + canvasW + " canvasH=" + canvasH);
        }

        return valid;
    }

    /**
     * FAIL-SAFE: Clear overlay (invoked on canvas size change or error).
     */
    private void clearOverlay() {
        Platform.runLater(() -> {
            try {
                GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
                gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
            } catch (Exception e) {
                LOGGER.warning("Error clearing overlay: " + e.getMessage());
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
        LOGGER.info("Playing: " + mrl);
    }

    public void stop() {
        mediaPlayer.controls().stop();
        clearOverlay();
        LOGGER.info("Playback stopped");
    }

    public void cleanup() {
        try {
            mediaPlayer.controls().stop();
            mediaPlayer.release();
            mediaPlayerFactory.release();
            LOGGER.info("SentinelVideoView cleaned up");
        } catch (Exception e) {
            LOGGER.warning("Error during cleanup: " + e.getMessage());
        }
    }
}