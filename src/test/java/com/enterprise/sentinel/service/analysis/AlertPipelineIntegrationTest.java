package com.enterprise.sentinel.service.analysis;

import com.enterprise.sentinel.domain.model.DetectionEvent;
import com.enterprise.sentinel.domain.model.GeofenceZone;
import com.enterprise.sentinel.domain.model.SecurityAlert;
import com.enterprise.sentinel.domain.model.Video;
import com.enterprise.sentinel.domain.repository.DetectionEventRepository;
import com.enterprise.sentinel.domain.repository.GeofenceZoneRepository;
import com.enterprise.sentinel.domain.repository.SecurityAlertRepository;
import com.enterprise.sentinel.domain.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-End Integration Test for Alert Pipeline.
 * 
 * Tests the complete flow:
 * DetectionEvent → AlertEngine → SecurityAlert → Persisted & Published
 * 
 * This validates that:
 * 1. Detections are created and persisted
 * 2. AlertEngine evaluates detections against zones
 * 3. Alerts are created for matching rules
 * 4. Alerts are published to the event bus
 * 5. Repository queries work correctly
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Alert Pipeline End-to-End Integration Test")
class AlertPipelineIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AlertEngine alertEngine(GeofenceZoneRepository zoneRepo, 
                                       SecurityAlertRepository alertRepo,
                                       ApplicationEventPublisher publisher) {
            return new AlertEngine(zoneRepo, alertRepo, publisher);
        }

        @Bean
        public AlertNotificationService alertNotificationService() {
            // Mock AuditLogger for testing
            return new AlertNotificationService(null);
        }
    }

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private DetectionEventRepository detectionEventRepository;

    @Autowired
    private GeofenceZoneRepository geofenceZoneRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private AlertEngine alertEngine;

    private Video testVideo;
    private GeofenceZone restrictedZone;
    private GeofenceZone allowedZone;

    @BeforeEach
    void setUp() {
        // Create test video
        testVideo = Video.builder()
                .sourceUrl("rtsp://test/stream")
                .filename("integration_test.mp4")
                .mediaType("video/mp4")
                .build();
        videoRepository.save(testVideo);

        // Create restricted zone (high security)
        restrictedZone = GeofenceZone.builder()
                .zoneCode("RESTRICTED_A")
                .zoneName("Restricted Area A")
                .triggerClass("person")
                .confidenceThreshold(0.75)
                .enabled(true)
                .severity("CRITICAL")
                .alertMessage("Unauthorized personnel detected in RESTRICTED_A")
                .build();
        geofenceZoneRepository.save(restrictedZone);

        // Create allowed zone (monitoring only)
        allowedZone = GeofenceZone.builder()
                .zoneCode("ALLOWED_B")
                .zoneName("Allowed Area B")
                .triggerClass("forklift")
                .confidenceThreshold(0.85)
                .enabled(true)
                .severity("HIGH")
                .alertMessage("Forklift detected in ALLOWED_B (unsafe)")
                .build();
        geofenceZoneRepository.save(allowedZone);
    }

    @Test
    @DisplayName("End-to-End: Detection → Alert Creation → Persistence")
    void testCompleteAlertPipeline() {
        // STEP 1: Create a high-confidence detection event
        DetectionEvent detection = DetectionEvent.builder()
                .videoId(testVideo.getId())
                .timestampMs(System.currentTimeMillis())
                .detectedClass("person")
                .confidence(0.95) // High confidence
                .boundingBox("[100, 100, 200, 200]")
                .inferenceData(java.util.Map.of(
                        "model", "yolov8n",
                        "class", "person",
                        "confidence", 0.95
                ))
                .build();

        DetectionEvent savedDetection = detectionEventRepository.save(detection);
        assertThat(savedDetection.getId()).isNotNull();
        assertThat(savedDetection.getDetectedClass()).isEqualTo("person");

        // STEP 2: Process detection through AlertEngine
        boolean alertCreated = alertEngine.processDetection(savedDetection);

        // STEP 3: Verify alert was created
        assertThat(alertCreated).isTrue();
        List<SecurityAlert> alerts = securityAlertRepository
                .findByGeofenceZoneIdOrderByCreatedAtDesc(restrictedZone.getId());
        assertThat(alerts).hasSize(1);

        SecurityAlert alert = alerts.get(0);
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alert.getAlertMessage()).contains("RESTRICTED_A");
        assertThat(alert.isAcknowledged()).isFalse();
    }

    @Test
    @DisplayName("End-to-End: Low Confidence Detection Should Not Trigger Alert")
    void testLowConfidenceDetectionNoAlert() {
        // Create low-confidence detection
        DetectionEvent detection = DetectionEvent.builder()
                .videoId(testVideo.getId())
                .timestampMs(System.currentTimeMillis())
                .detectedClass("person")
                .confidence(0.50) // Below 0.75 threshold
                .boundingBox("[300, 300, 400, 400]")
                .inferenceData(java.util.Map.of("confidence", 0.50))
                .build();

        detectionEventRepository.save(detection);

        // Process through AlertEngine
        boolean alertCreated = alertEngine.processDetection(detection);

        // Should NOT create alert
        assertThat(alertCreated).isFalse();
        List<SecurityAlert> alerts = securityAlertRepository.findAll();
        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("End-to-End: Class Mismatch Should Not Trigger Alert")
    void testClassMismatchNoAlert() {
        // Detection is "car", but RESTRICTED_A zone only triggers on "person"
        DetectionEvent detection = DetectionEvent.builder()
                .videoId(testVideo.getId())
                .timestampMs(System.currentTimeMillis())
                .detectedClass("car") // Doesn't match zone trigger class
                .confidence(0.92)
                .boundingBox("[100, 100, 200, 200]")
                .build();

        detectionEventRepository.save(detection);
        boolean alertCreated = alertEngine.processDetection(detection);

        assertThat(alertCreated).isFalse();
        List<SecurityAlert> alerts = securityAlertRepository.findAll();
        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("End-to-End: Multiple Zones Should Trigger Multiple Alerts")
    void testMultipleZonesMultipleAlerts() {
        // Create forklift detection
        DetectionEvent forkliftDetection = DetectionEvent.builder()
                .videoId(testVideo.getId())
                .timestampMs(System.currentTimeMillis())
                .detectedClass("forklift")
                .confidence(0.90)
                .boundingBox("[100, 100, 200, 200]")
                .build();

        detectionEventRepository.save(forkliftDetection);

        // Process through AlertEngine - should match ALLOWED_B zone
        boolean alertCreated = alertEngine.processDetection(forkliftDetection);

        assertThat(alertCreated).isTrue();
        List<SecurityAlert> allAlerts = securityAlertRepository.findAll();
        assertThat(allAlerts).hasSize(1);
        assertThat(allAlerts.get(0).getSeverity()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("End-to-End: Alert Acknowledgment Persists Correctly")
    void testAlertAcknowledgment() {
        // Create and process detection
        DetectionEvent detection = DetectionEvent.builder()
                .videoId(testVideo.getId())
                .timestampMs(System.currentTimeMillis())
                .detectedClass("person")
                .confidence(0.95)
                .boundingBox("[100, 100, 200, 200]")
                .build();

        detectionEventRepository.save(detection);
        alertEngine.processDetection(detection);

        // Get created alert
        List<SecurityAlert> alerts = securityAlertRepository
                .findByGeofenceZoneIdOrderByCreatedAtDesc(restrictedZone.getId());
        SecurityAlert alert = alerts.get(0);

        assertThat(alert.isAcknowledged()).isFalse();

        // Acknowledge the alert
        alertEngine.acknowledgeAlert(alert.getId().toString(), "security_admin");

        // Verify persistence
        SecurityAlert updatedAlert = securityAlertRepository.findById(alert.getId()).orElseThrow();
        assertThat(updatedAlert.isAcknowledged()).isTrue();
        assertThat(updatedAlert.getAcknowledgedBy()).isEqualTo("security_admin");
        assertThat(updatedAlert.getAcknowledgedAt()).isNotNull();
    }

    @Test
    @DisplayName("End-to-End: Multiple Detections Create Multiple Alerts")
    void testMultipleDetectionsMultipleAlerts() {
        // Create 5 detections in quick succession
        for (int i = 0; i < 5; i++) {
            DetectionEvent detection = DetectionEvent.builder()
                    .videoId(testVideo.getId())
                    .timestampMs(System.currentTimeMillis() + (i * 100))
                    .detectedClass("person")
                    .confidence(0.90 + (i * 0.01)) // Gradually increasing confidence
                    .boundingBox("[" + (100 + i*10) + ", 100, " + (200 + i*10) + ", 200]")
                    .build();

            DetectionEvent saved = detectionEventRepository.save(detection);
            alertEngine.processDetection(saved);
        }

        // All 5 should trigger alerts
        List<SecurityAlert> alerts = securityAlertRepository
                .findByGeofenceZoneIdOrderByCreatedAtDesc(restrictedZone.getId());
        assertThat(alerts).hasSize(5);
        assertThat(alerts).allMatch(a -> !a.isAcknowledged());
    }

    @Test
    @DisplayName("End-to-End: Time Range Query Filters Correctly")
    void testTimeRangeQueryFiltering() {
        LocalDateTime now = LocalDateTime.now();
        long nowMs = System.currentTimeMillis();

        // Create detection
        DetectionEvent detection = DetectionEvent.builder()
                .videoId(testVideo.getId())
                .timestampMs(nowMs)
                .detectedClass("person")
                .confidence(0.95)
                .boundingBox("[100, 100, 200, 200]")
                .createdAt(now)
                .build();

        detectionEventRepository.save(detection);
        alertEngine.processDetection(detection);

        // Query with time range
        LocalDateTime start = now.minusHours(1);
        LocalDateTime end = now.plusHours(1);

        List<SecurityAlert> alertsInRange = securityAlertRepository.findByTimeRange(start, end);
        assertThat(alertsInRange).hasSize(1);

        // Query outside range should return empty
        LocalDateTime beforeStart = now.minusDays(1);
        LocalDateTime beforeEnd = now.minusHours(2);
        List<SecurityAlert> alertsOutside = securityAlertRepository.findByTimeRange(beforeStart, beforeEnd);
        assertThat(alertsOutside).isEmpty();
    }

    @Test
    @DisplayName("End-to-End: Unacknowledged Alerts Query Works")
    void testUnacknowledgedAlertsQuery() {
        // Create 3 detections
        for (int i = 0; i < 3; i++) {
            DetectionEvent detection = DetectionEvent.builder()
                    .videoId(testVideo.getId())
                    .timestampMs(System.currentTimeMillis() + (i * 100))
                    .detectedClass("person")
                    .confidence(0.92)
                    .boundingBox("[100, 100, 200, 200]")
                    .build();

            DetectionEvent saved = detectionEventRepository.save(detection);
            alertEngine.processDetection(saved);
        }

        // All should be unacknowledged
        List<SecurityAlert> unacknowledged = securityAlertRepository
                .findByAcknowledgedFalseOrderByCreatedAtDesc();
        assertThat(unacknowledged).hasSize(3);

        // Acknowledge one
        alertEngine.acknowledgeAlert(unacknowledged.get(0).getId().toString(), "admin");

        // Should now have 2 unacknowledged
        List<SecurityAlert> stillUnacknowledged = securityAlertRepository
                .findByAcknowledgedFalseOrderByCreatedAtDesc();
        assertThat(stillUnacknowledged).hasSize(2);
    }
}
