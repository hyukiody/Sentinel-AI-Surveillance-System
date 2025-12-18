package com.enterprise.sentinel.client.video;

import javafx.application.Platform;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Phase 2: UI-01 Rendering Pipeline
 * 
 * High-performance, fail-proof integration between VLC (Native) and JavaFX (ImageView).
 * Uses AtomicReference for thread-safe image buffering and fail-safe guards.
 * 
 * Data Flow: Native Buffer → JavaFxVideoSurface → AtomicReference → UI Consumer
 * 
 * Thread Safety: All state transitions use atomic types (AtomicReference, AtomicLong).
 * Fail-Safe: Buffer validation, null checks, and graceful frame drops.
 */
public class JavaFxVideoSurface extends CallbackVideoSurface {

    private static final Logger LOGGER = Logger.getLogger(JavaFxVideoSurface.class.getName());

    public JavaFxVideoSurface(Consumer<WritableImage> imageConsumer) {
        super(new JavaFxBufferFormatCallback(), new JavaFxRenderCallback(imageConsumer), true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
    }

    private static class JavaFxBufferFormatCallback implements BufferFormatCallback {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            // FAIL-SAFE: Validate dimensions
            if (sourceWidth <= 0 || sourceHeight <= 0) {
                LOGGER.warning("Invalid video dimensions: " + sourceWidth + "x" + sourceHeight);
                return new RV32BufferFormat(1920, 1080); // Default fallback
            }
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            // FAIL-SAFE: Validate allocated buffers
            if (buffers == null || buffers.length == 0) {
                LOGGER.warning("No buffers allocated by VLC");
                return;
            }
            LOGGER.fine("VLC allocated " + buffers.length + " buffer(s)");
        }
    }

    private static class JavaFxRenderCallback implements RenderCallback {
        private final Consumer<WritableImage> onImageReady;
        private final AtomicReference<PixelBuffer<ByteBuffer>> pixelBufferRef = new AtomicReference<>(null);
        private final AtomicReference<WritableImage> imageRef = new AtomicReference<>(null);
        private final AtomicLong frameCount = new AtomicLong(0L);
        private final AtomicLong droppedFrameCount = new AtomicLong(0L);

        public JavaFxRenderCallback(Consumer<WritableImage> onImageReady) {
            this.onImageReady = onImageReady;
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            // FAIL-SAFE: Validate input
            if (nativeBuffers == null || nativeBuffers.length == 0) {
                LOGGER.warning("Received null or empty native buffers");
                droppedFrameCount.incrementAndGet();
                return;
            }

            ByteBuffer nativeBuffer = nativeBuffers[0];
            if (nativeBuffer == null || nativeBuffer.capacity() == 0) {
                LOGGER.warning("Native buffer is null or empty");
                droppedFrameCount.incrementAndGet();
                return;
            }

            // Initialize pixel buffer on first frame
            if (pixelBufferRef.get() == null) {
                initPixelBuffer(bufferFormat.getWidth(), bufferFormat.getHeight(), nativeBuffer);
            }

            // ATOMIC TRANSITION: Update image on FX thread
            Platform.runLater(() -> {
                try {
                    PixelBuffer<ByteBuffer> pixelBuffer = pixelBufferRef.get();
                    
                    // FAIL-SAFE: Verify pixel buffer is still valid
                    if (pixelBuffer == null) {
                        LOGGER.warning("Pixel buffer became null during frame processing");
                        return;
                    }

                    // Tell JavaFX pixels changed
                    pixelBuffer.updateBuffer(b -> null);

                    WritableImage currentImage = imageRef.get();
                    
                    // GUARANTEE: Image is either valid or null
                    if (currentImage != null) {
                        onImageReady.accept(currentImage);
                        frameCount.incrementAndGet();
                        
                        // Log performance metrics every 60 frames
                        long count = frameCount.get();
                        if (count % 60 == 0) {
                            long dropped = droppedFrameCount.get();
                            LOGGER.info("Rendered " + count + " frames, dropped " + dropped);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.severe("Error updating pixel buffer: " + e.getMessage());
                    droppedFrameCount.incrementAndGet();
                }
            });
        }

        private void initPixelBuffer(int width, int height, ByteBuffer nativeBuffer) {
            try {
                // FAIL-SAFE: Validate dimensions
                if (width <= 0 || height <= 0) {
                    LOGGER.warning("Invalid pixel buffer dimensions: " + width + "x" + height);
                    return;
                }

                // FAIL-SAFE: Validate buffer capacity
                int expectedCapacity = width * height * 4; // BGRA = 4 bytes/pixel
                if (nativeBuffer.capacity() < expectedCapacity) {
                    LOGGER.warning("Buffer size mismatch: " + nativeBuffer.capacity() + " < " + expectedCapacity);
                    return;
                }

                // Create pixel buffer (zero-copy with native memory)
                PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(
                    width, height, nativeBuffer, PixelFormat.getByteBgraPreInstance()
                );
                pixelBufferRef.set(pixelBuffer);

                // Create writable image
                WritableImage writableImage = new WritableImage(pixelBuffer);
                imageRef.set(writableImage);

                LOGGER.info("Initialized pixel buffer: " + width + "x" + height);

                // Initial callback
                Platform.runLater(() -> {
                    WritableImage img = imageRef.get();
                    if (img != null) {
                        onImageReady.accept(img);
                    }
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to initialize pixel buffer: " + e.getMessage());
            }
        }

        public long getFrameCount() {
            return frameCount.get();
        }

        public long getDroppedFrameCount() {
            return droppedFrameCount.get();
        }
    }
}