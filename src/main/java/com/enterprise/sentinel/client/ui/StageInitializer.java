package com.enterprise.sentinel.client.ui;

import com.enterprise.sentinel.client.StageReadyEvent;
import com.enterprise.sentinel.service.ingestion.FileIngestionService;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;
import com.enterprise.sentinel.service.analysis.VideoProcessor;
@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final FileIngestionService fileIngestionService;
    private final VideoProcessor videoProcessor; // ADD

    // Spring dependency injection works here!
    public StageInitializer(FileIngestionService fileIngestionService, VideoProcessor videoProcessor) {
        this.fileIngestionService = fileIngestionService;
        this.videoProcessor = videoProcessor;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        
        // 1. Main Layout (BorderPane allows Top/Center/Bottom)
        BorderPane root = new BorderPane();
        
        // 2. Video View (Center)
        SentinelVideoView videoView = new SentinelVideoView();
        root.setCenter(videoView);
        
        //wiring start
        videoProcessor.setVideoView(videoView); // Allow processor to draw on view
        videoView.setOnFrameReady(videoProcessor::processFrame); // Feed frames to processor
        
        //wiring end
        // 3. Menu Bar (Top)
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
     // NEW: Stream Menu
        Menu streamMenu = new Menu("Stream");
        MenuItem connectItem = new MenuItem("Connect to RTSP...");
        
        connectItem.setOnAction(e -> {
            // Simple Input Dialog
            TextInputDialog dialog = new TextInputDialog("rtsp://");
            dialog.setTitle("Connect to Live Stream");
            dialog.setHeaderText("Enter RTSP URL");
            dialog.setContentText("URL:");

            dialog.showAndWait().ifPresent(url -> {
                try {
                    // Just play it directly via our updated View
                    System.out.println("📡 Connecting to Stream: " + url);
                    videoView.play(url);
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connection Error");
                    alert.setHeaderText("Failed to connect to stream");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            });
        });

        streamMenu.getItems().add(connectItem);
        menuBar.getMenus().add(streamMenu); // Add Stream menu
        MenuItem openItem = new MenuItem("Open Video...");
        
        openItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Surveillance Video");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv")
            );
            
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                try {
                    // Use Backend Service to validate/prepare
                    String mrl = fileIngestionService.prepareFileForPlayback(selectedFile);
                    System.out.println("▶ Playing: " + mrl);
                    videoView.play(mrl);
                } catch (Exception ex) {
                    System.err.println("Error loading file: " + ex.getMessage());
                }
            }
        });

        fileMenu.getItems().add(openItem);
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        // 4. Scene Setup
        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        stage.setTitle("Sentinel AI Surveillance - JavaFX");
        stage.setOnCloseRequest(e -> videoView.cleanup());
        stage.show();
    }
}