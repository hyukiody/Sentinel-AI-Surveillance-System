package com.enterprise.sentinel.service.analysis;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom Translator for YOLOv8 ONNX models.
 * Fixed for standard [1, 84, 8400] output layout.
 */
public class YoloV8Translator implements Translator<Image, DetectedObjects> {

    private final List<String> classNameList;
    private final float confidenceThreshold;
    private final float nmsThreshold;

    public YoloV8Translator(List<String> classNameList, float confidenceThreshold, float nmsThreshold) {
        this.classNameList = classNameList;
        this.confidenceThreshold = confidenceThreshold;
        this.nmsThreshold = nmsThreshold;
    }

    @Override
    public Batchifier getBatchifier() {
        return null; // Disable batching
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        BufferedImage resized = resizeImage(input, 640, 640);
        FloatBuffer buffer = imageToFloatBuffer(resized);
        NDManager manager = ctx.getNDManager();
        ai.djl.ndarray.types.Shape shape = new ai.djl.ndarray.types.Shape(1, 3, 640, 640);
        NDArray array = manager.create(shape, ai.djl.ndarray.types.DataType.FLOAT32);
        array.set(buffer);
        return new NDList(array);
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
        // Flatten the tensor [1, 84, 8400] -> flat float[]
        float[] flatOutput = list.get(0).toFloatArray();

        // Standard YOLOv8 Output Layout: [1, 84, 8400]
        // 84 Rows (0=cx, 1=cy, 2=w, 3=h, 4..83=Classes)
        // 8400 Columns (Anchors)
        // Memory Layout: Row 0 [0..8399], Row 1 [8400..16799], etc.
        int numAnchors = 8400; 
        
        List<IntermediateResult> candidates = new ArrayList<>();

        // Iterate through all 8400 anchors (columns)
        for (int i = 0; i < numAnchors; i++) {
            // 1. Find best class (Rows 4 to 83)
            int classId = -1;
            float maxProb = -1f;
            
            for (int c = 0; c < 80; c++) {
                // Calculate index for: Row=(4+c), Column=i
                int index = (4 + c) * numAnchors + i;
                
                if (index >= flatOutput.length) continue; 
                
                float prob = flatOutput[index];
                if (prob > maxProb) {
                    maxProb = prob;
                    classId = c;
                }
            }

            if (maxProb < confidenceThreshold) continue;

            // 2. Extract Coords (Rows 0-3)
            // Stride by numAnchors to jump between rows
            float cx = flatOutput[0 * numAnchors + i]; // Row 0
            float cy = flatOutput[1 * numAnchors + i]; // Row 1
            float w  = flatOutput[2 * numAnchors + i]; // Row 2
            float h  = flatOutput[3 * numAnchors + i]; // Row 3

            // 3. Normalize & Convert (Center -> Top-Left)
            float x = (cx - w / 2f) / 640f;
            float y = (cy - h / 2f) / 640f;
            float width = w / 640f;
            float height = h / 640f;
            
            x = Math.max(0, Math.min(1, x));
            y = Math.max(0, Math.min(1, y));
            width = Math.min(1 - x, width);
            height = Math.min(1 - y, height);

            candidates.add(new IntermediateResult(classId, maxProb, x, y, width, height));
        }

        // 3. Apply NMS
        List<IntermediateResult> kept = nms(candidates);

        // 4. Build Final Result
        List<String> names = new ArrayList<>();
        List<Double> probs = new ArrayList<>();
        List<ai.djl.modality.cv.output.BoundingBox> boxes = new ArrayList<>();

        for (IntermediateResult res : kept) {
            names.add(classNameList.get(res.classId));
            probs.add((double) res.prob);
            boxes.add(new Rectangle(res.x, res.y, res.w, res.h));
        }

        return new DetectedObjects(names, probs, boxes);
    }

    // --- Helpers (NMS & Image) ---
    
    private List<IntermediateResult> nms(List<IntermediateResult> candidates) {
        List<IntermediateResult> result = new ArrayList<>();
        candidates.sort(Comparator.comparingDouble(a -> -a.prob));
        while (!candidates.isEmpty()) {
            IntermediateResult best = candidates.remove(0);
            result.add(best);
            candidates.removeIf(other -> calculateIoU(best, other) > nmsThreshold);
        }
        return result;
    }

    private float calculateIoU(IntermediateResult boxA, IntermediateResult boxB) {
        float xA = Math.max(boxA.x, boxB.x);
        float yA = Math.max(boxA.y, boxB.y);
        float xB = Math.min(boxA.x + boxA.w, boxB.x + boxB.w);
        float yB = Math.min(boxA.y + boxA.h, boxB.y + boxB.h);
        float interW = Math.max(0, xB - xA);
        float interH = Math.max(0, yB - yA);
        float interArea = interW * interH;
        float boxAArea = boxA.w * boxA.h;
        float boxBArea = boxB.w * boxB.h;
        return interArea / (boxAArea + boxBArea - interArea);
    }

    private static class IntermediateResult {
        int classId; float prob, x, y, w, h;
        public IntermediateResult(int c, float p, float x, float y, float w, float h) {
            this.classId=c; this.prob=p; this.x=x; this.y=y; this.w=w; this.h=h;
        }
    }

    private BufferedImage resizeImage(Image input, int width, int height) {
        BufferedImage original = (BufferedImage) input.getWrappedImage();
        if (original.getWidth() == width && original.getHeight() == height) return original;
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private FloatBuffer imageToFloatBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        FloatBuffer buffer = FloatBuffer.allocate(3 * width * height);
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (int c = 0; c < 3; c++) {
            for (int i = 0; i < pixels.length; i++) {
                int value = (pixels[i] >> (16 - 8 * c)) & 0xFF;
                buffer.put(value / 255.0f);
            }
        }
        buffer.rewind();
        return buffer;
    }
}