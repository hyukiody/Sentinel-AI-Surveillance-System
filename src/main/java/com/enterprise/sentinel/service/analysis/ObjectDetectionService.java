package com.enterprise.sentinel.service.analysis;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

@Service
public class ObjectDetectionService {

    private ZooModel<Image, DetectedObjects> model;
    
    // Standard COCO dataset classes (80 objects)
    private static final List<String> COCO_CLASSES = Arrays.asList(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
        "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
        "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
        "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    );

    @PostConstruct
    public void init() throws ModelException, IOException {
        // FORCE ONNX ENGINE (Prevents PyTorch lookup errors)
        System.setProperty("ai.djl.default_engine", "OnnxRuntime");
        
        System.out.println("🧠 Initializing AI Engine (ONNX)...");

        Path modelDir = Path.of("models");
        Path modelFile = modelDir.resolve("yolov8n.onnx");

        // Download logic hidden for brevity, assuming file exists or downloads
        if (!Files.exists(modelFile)) {
             downloadModel(modelFile);
        }

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optEngine("OnnxRuntime")
                .optModelPath(modelFile)
                .optTranslator(new YoloV8Translator(COCO_CLASSES, 0.5f, 0.45f))
                .build();
        
        this.model = criteria.loadModel();
        System.out.println("✅ YOLOv8 AI Core Ready!");
    }

    private void downloadModel(Path modelFile) throws IOException {
        System.out.println("📥 Downloading YOLOv8n ONNX model (~12 MB)...");
        Files.createDirectories(modelFile.getParent());
        String modelUrl = "https://huggingface.co/dosage/yolov8n-onnx/resolve/main/yolov8n.onnx";
        try (InputStream in = new URL(modelUrl).openStream()) {
            Files.copy(in, modelFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public DetectedObjects detect(Image image) {
        try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            return predictor.predict(image);
        } catch (Exception e) {
            throw new RuntimeException("Inference failed", e);
        }
    }

    @PreDestroy
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}