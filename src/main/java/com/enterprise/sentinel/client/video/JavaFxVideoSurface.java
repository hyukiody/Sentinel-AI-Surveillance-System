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
import java.util.function.Consumer;

/**
 * High-performance integration between VLC (Native) and JavaFX (ImageView).
 * FIX: Now triggers the imageConsumer on every frame to drive the AI loop.
 */
public class JavaFxVideoSurface extends CallbackVideoSurface {

    public JavaFxVideoSurface(Consumer<WritableImage> imageConsumer) {
        super(new JavaFxBufferFormatCallback(), new JavaFxRenderCallback(imageConsumer), true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
    }

    private static class JavaFxBufferFormatCallback implements BufferFormatCallback {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            // No custom allocation needed
        }
    }

    private static class JavaFxRenderCallback implements RenderCallback {
        private final Consumer<WritableImage> onImageReady;
        private PixelBuffer<ByteBuffer> pixelBuffer;
        private WritableImage writableImage; // FIX: Store this as a field

        public JavaFxRenderCallback(Consumer<WritableImage> onImageReady) {
            this.onImageReady = onImageReady;
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            if (pixelBuffer == null) {
                initPixelBuffer(bufferFormat.getWidth(), bufferFormat.getHeight(), nativeBuffers[0]);
            }
            
            Platform.runLater(() -> {
                if (pixelBuffer != null) {
                    // 1. Tell JavaFX pixels changed
                    pixelBuffer.updateBuffer(b -> null);
                    
                    // 2. FIX: Notify the UI/AI that a new frame is ready!
                    // This triggers the VideoProcessor loop repeatedly.
                    if (writableImage != null) {
                        onImageReady.accept(writableImage);
                    }
                }
            });
        }

        private void initPixelBuffer(int width, int height, ByteBuffer nativeBuffer) {
            pixelBuffer = new PixelBuffer<>(width, height, nativeBuffer, PixelFormat.getByteBgraPreInstance());
            writableImage = new WritableImage(pixelBuffer);
            
            // Initial call
            Platform.runLater(() -> onImageReady.accept(writableImage));
        }
    }
}