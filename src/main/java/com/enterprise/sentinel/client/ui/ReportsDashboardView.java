package com.enterprise.sentinel.client.ui;

import com.enterprise.sentinel.domain.model.SecurityAlert;
import com.enterprise.sentinel.service.analysis.AlertNotificationService;
import com.enterprise.sentinel.service.analysis.AnalyticsService;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Reports Dashboard View - JavaFX UI component for displaying security alerts and analytics.
 * 
 * Tabs:
 * 1. Live Alerts - Real-time alert queue from AlertNotificationService
 * 2. Heatmaps - Spatial distribution of detections
 * 3. Dwell Time - Temporal analysis of objects in zones
 * 4. Compliance - Zone violation reports
 * 5. Analytics - Detection statistics and confidence metrics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportsDashboardView {

    private final AlertNotificationService alertNotificationService;
    private final AnalyticsService analyticsService;

    private TabPane tabPane;
    private Label alertCountLabel;
    private ListView<String> liveAlertsListView;
    private Timer refreshTimer;

    /**
     * Build the complete dashboard UI.
     */
    public VBox buildDashboard() {
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(10);
        root.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1;");

        // Header with title and unacknowledged count
        HBox header = buildHeader();
        
        // Tab pane with all reports
        tabPane = buildTabPane();

        root.getChildren().addAll(header, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Start background refresh timer (every 30 seconds)
        startRefreshTimer();

        return root;
    }

    /**
     * Build header with title and alert count indicator.
     */
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setSpacing(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("Security Reports Dashboard");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#333"));

        alertCountLabel = new Label("Unacknowledged: 0");
        alertCountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        alertCountLabel.setTextFill(Color.web("#d32f2f"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("🔄 Refresh");
        refreshButton.setPrefWidth(100);
        refreshButton.setOnAction(e -> refreshAllTabs());

        header.getChildren().addAll(titleLabel, spacer, alertCountLabel, refreshButton);
        return header;
    }

    /**
     * Build tab pane with all report tabs.
     */
    private TabPane buildTabPane() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-font-size: 12;");

        tabs.getTabs().addAll(
                buildLiveAlertsTab(),
                buildHeatmapTab(),
                buildDwellTimeTab(),
                buildComplianceTab(),
                buildAnalyticsTab()
        );

        return tabs;
    }

    /**
     * TAB 1: Live Alerts - Real-time alert queue.
     */
    private Tab buildLiveAlertsTab() {
        Tab tab = new Tab("🚨 Live Alerts", buildLiveAlertsContent());
        tab.setClosable(false);
        return tab;
    }

    private VBox buildLiveAlertsContent() {
        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.setSpacing(10);

        Label descLabel = new Label("Real-time security alerts (auto-refreshes every 10 seconds)");
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        liveAlertsListView = new ListView<>();
        liveAlertsListView.setPrefHeight(300);
        liveAlertsListView.setCellFactory(lv -> new AlertListCell());

        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button clearButton = new Button("Clear All");
        clearButton.setOnAction(e -> {
            liveAlertsListView.getItems().clear();
            alertNotificationService.clearAlertQueue();
        });

        Button acknowledgeButton = new Button("Acknowledge Selected");
        acknowledgeButton.setOnAction(e -> acknowledgeSelectedAlert());

        buttonBox.getChildren().addAll(acknowledgeButton, clearButton);

        content.getChildren().addAll(descLabel, liveAlertsListView, buttonBox);
        VBox.setVgrow(liveAlertsListView, Priority.ALWAYS);

        return content;
    }

    /**
     * TAB 2: Heatmap - Spatial detection distribution.
     */
    private Tab buildHeatmapTab() {
        Tab tab = new Tab("🔥 Heatmaps", buildHeatmapContent());
        tab.setClosable(false);
        return tab;
    }

    private VBox buildHeatmapContent() {
        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.setSpacing(10);

        HBox controlBox = new HBox();
        controlBox.setSpacing(10);
        controlBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> classCombo = new ComboBox<>();
        classCombo.getItems().addAll("person", "car", "bicycle", "truck");
        classCombo.setValue("person");
        classCombo.setPrefWidth(100);

        Label label = new Label("Object Class:");
        label.setStyle("-fx-font-weight: bold;");

        Button generateButton = new Button("Generate Heatmap");
        generateButton.setOnAction(e -> {
            String selectedClass = classCombo.getValue();
            log.info("Generating heatmap for: {}", selectedClass);
            // In production: Fetch heatmap data via AnalyticsService and visualize
        });

        controlBox.getChildren().addAll(label, classCombo, generateButton);

        TextArea heatmapDisplay = new TextArea();
        heatmapDisplay.setWrapText(true);
        heatmapDisplay.setPrefHeight(250);
        heatmapDisplay.setText("Heatmap visualization will display here.\n\nSpatial grid showing detection density across video frame.");

        content.getChildren().addAll(controlBox, heatmapDisplay);
        VBox.setVgrow(heatmapDisplay, Priority.ALWAYS);

        return content;
    }

    /**
     * TAB 3: Dwell Time - Temporal tracking in zones.
     */
    private Tab buildDwellTimeTab() {
        Tab tab = new Tab("⏱️ Dwell Time", buildDwellTimeContent());
        tab.setClosable(false);
        return tab;
    }

    private VBox buildDwellTimeContent() {
        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.setSpacing(10);

        Label descLabel = new Label("Track time spent by objects in restricted zones");
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        // Simple bar chart showing dwell times
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Object Tracks");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Time (seconds)");

        BarChart<String, Number> dwellChart = new BarChart<>(xAxis, yAxis);
        dwellChart.setTitle("Dwell Time by Track");
        dwellChart.setAnimated(true);
        dwellChart.setPrefHeight(300);

        // Populate with sample data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Dwell Time");
        series.getData().addAll(
                new XYChart.Data<>("Track 1", 45),
                new XYChart.Data<>("Track 2", 23),
                new XYChart.Data<>("Track 3", 67),
                new XYChart.Data<>("Track 4", 12)
        );
        dwellChart.getData().add(series);

        content.getChildren().addAll(descLabel, dwellChart);
        VBox.setVgrow(dwellChart, Priority.ALWAYS);

        return content;
    }

    /**
     * TAB 4: Compliance - Zone violation reports.
     */
    private Tab buildComplianceTab() {
        Tab tab = new Tab("⚠️ Compliance", buildComplianceContent());
        tab.setClosable(false);
        return tab;
    }

    private VBox buildComplianceContent() {
        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.setSpacing(10);

        Label descLabel = new Label("Restricted zone violations and compliance violations");
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        TableView<Map<String, String>> violationTable = new TableView<>();

        TableColumn<Map<String, String>, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().get("time"), data.getValue()));
        timeCol.setPrefWidth(150);

        TableColumn<Map<String, String>, String> classCol = new TableColumn<>("Object Class");
        classCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().get("class"), data.getValue()));
        classCol.setPrefWidth(100);

        TableColumn<Map<String, String>, String> zoneCol = new TableColumn<>("Zone");
        zoneCol.setCellValueFactory(data -> javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().get("zone"), data.getValue()));
        zoneCol.setPrefWidth(100);

        violationTable.getColumns().addAll(timeCol, classCol, zoneCol);
        violationTable.setPrefHeight(300);

        content.getChildren().addAll(descLabel, violationTable);
        VBox.setVgrow(violationTable, Priority.ALWAYS);

        return content;
    }

    /**
     * TAB 5: Analytics - Detection statistics and confidence.
     */
    private Tab buildAnalyticsTab() {
        Tab tab = new Tab("📊 Analytics", buildAnalyticsContent());
        tab.setClosable(false);
        return tab;
    }

    private VBox buildAnalyticsContent() {
        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.setSpacing(10);

        // Detection frequency chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Object Class");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Detection Count");

        BarChart<String, Number> frequencyChart = new BarChart<>(xAxis, yAxis);
        frequencyChart.setTitle("Detection Frequency by Class");
        frequencyChart.setAnimated(true);
        frequencyChart.setPrefHeight(250);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Detections");
        series.getData().addAll(
                new XYChart.Data<>("Person", 245),
                new XYChart.Data<>("Car", 87),
                new XYChart.Data<>("Bicycle", 34),
                new XYChart.Data<>("Truck", 12)
        );
        frequencyChart.getData().add(series);

        // Statistics panel
        HBox statsBox = new HBox();
        statsBox.setSpacing(15);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-width: 1;");

        statsBox.getChildren().addAll(
                createStatCard("Avg Confidence", "0.92", "#4CAF50"),
                createStatCard("Total Detections", "2,847", "#2196F3"),
                createStatCard("Alerts Triggered", "34", "#FF9800"),
                createStatCard("Compliance Rate", "97.3%", "#8BC34A")
        );

        content.getChildren().addAll(statsBox, frequencyChart);
        VBox.setVgrow(frequencyChart, Priority.ALWAYS);

        return content;
    }

    /**
     * Helper to create stat cards.
     */
    private VBox createStatCard(String title, String value, String colorHex) {
        VBox card = new VBox();
        card.setSpacing(5);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: " + colorHex + 
                      "; -fx-border-width: 2; -fx-border-radius: 5;");
        card.setPrefWidth(150);
        card.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    /**
     * Refresh all tabs with latest data.
     */
    private void refreshAllTabs() {
        Platform.runLater(this::refreshLiveAlerts);
        log.info("Dashboard refreshed");
    }

    /**
     * Refresh live alerts from queue.
     */
    private void refreshLiveAlerts() {
        List<SecurityAlert> recentAlerts = alertNotificationService.getRecentAlerts(50);
        int unacknowledgedCount = alertNotificationService.getUnacknowledgedAlertCount();

        Platform.runLater(() -> {
            alertCountLabel.setText("Unacknowledged: " + unacknowledgedCount);
            alertCountLabel.setTextFill(unacknowledgedCount > 0 ? Color.web("#d32f2f") : Color.web("#2e7d32"));

            liveAlertsListView.getItems().clear();
            recentAlerts.forEach(alert -> {
                String displayText = String.format("[%s] %s - %s (severity: %s)",
                        alert.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                        alert.getSeverity(),
                        alert.getAlertMessage(),
                        alert.isAcknowledged() ? "✓ Acknowledged" : "⏳ Pending");
                liveAlertsListView.getItems().add(displayText);
            });
        });
    }

    /**
     * Acknowledge selected alert.
     */
    private void acknowledgeSelectedAlert() {
        int selectedIndex = liveAlertsListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            // In production: Parse alert ID from display text and call AlertEngine.acknowledgeAlert()
            liveAlertsListView.getItems().remove(selectedIndex);
            log.info("Alert acknowledged");
        }
    }

    /**
     * Start background refresh timer (every 30 seconds).
     */
    private void startRefreshTimer() {
        refreshTimer = new Timer("DashboardRefreshTimer", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshLiveAlerts();
            }
        }, 10000, 30000); // Start after 10s, repeat every 30s
    }

    /**
     * Stop background refresh timer (cleanup).
     */
    public void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }

    /**
     * Custom cell renderer for alert list.
     */
    private static class AlertListCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item);
                // Color code by severity
                if (item.contains("CRITICAL")) {
                    setTextFill(Color.web("#d32f2f"));
                    setStyle("-fx-font-weight: bold;");
                } else if (item.contains("HIGH")) {
                    setTextFill(Color.web("#f57c00"));
                } else if (item.contains("MEDIUM")) {
                    setTextFill(Color.web("#fbc02d"));
                } else {
                    setTextFill(Color.web("#1976d2"));
                }
            }
        }
    }
}
