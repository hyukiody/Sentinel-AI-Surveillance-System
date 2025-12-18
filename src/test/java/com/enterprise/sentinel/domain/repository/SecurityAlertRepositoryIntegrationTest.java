package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.DetectionEvent;
import com.enterprise.sentinel.domain.model.GeofenceZone;
import com.enterprise.sentinel.domain.model.SecurityAlert;
import com.enterprise.sentinel.domain.model.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("SecurityAlertRepository Integration Tests")
class SecurityAlertRepositoryIntegrationTest {

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private GeofenceZoneRepository geofenceZoneRepository;

    @Autowired
    private DetectionEventRepository detectionEventRepository;

    @Autowired
    private VideoRepository videoRepository;

    private GeofenceZone testZone;
    private DetectionEvent testDetection;
    private Video testVideo;

    @BeforeEach
    void setUp() {
        // Create test video
        testVideo = Video.builder()
                .sourceUrl("rtsp://test/stream")
                .filename("test.mp4")
                .mediaType("video/mp4")
                .build();
        videoRepository.save(testVideo);

        // Create test geofence zone
        testZone = GeofenceZone.builder()
                .zoneCode("TEST_ZONE")
                .zoneName("Test Zone")
                .triggerClass("person")
                .confidenceThreshold(0.75)
                .enabled(true)
                .severity("HIGH")
                .alertMessage("Unauthorized person in test zone")
                .build();
        geofenceZoneRepository.save(testZone);

        // Create test detection event
        testDetection = DetectionEvent.builder()
                .videoId(testVideo.getId())
                .detectedClass("person")
                .confidence(0.95)
                .boundingBox("[100, 100, 200, 200]")
                .inferenceData("{\"model\": \"yolov8n\"}")
                .build();
        detectionEventRepository.save(testDetection);
    }

    @Test
    @DisplayName("Should save and retrieve SecurityAlert")
    void testSaveAndRetrieve() {
        // Arrange
        SecurityAlert alert = SecurityAlert.builder()
                .geofenceZoneId(testZone.getId())
                .detectionEventId(testDetection.getId())
                .severity("HIGH")
                .alertMessage("Test alert")
                .acknowledged(false)
                .build();

        // Act
        SecurityAlert saved = securityAlertRepository.save(alert);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(securityAlertRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Should find unacknowledged alerts")
    void testFindUnacknowledgedAlerts() {
        // Arrange
        SecurityAlert unacknowledged1 = createAlert(testZone, testDetection, false, null);
        SecurityAlert unacknowledged2 = createAlert(testZone, testDetection, false, null);
        SecurityAlert acknowledged = createAlert(testZone, testDetection, true, "admin");

        securityAlertRepository.saveAll(List.of(unacknowledged1, unacknowledged2, acknowledged));

        // Act
        List<SecurityAlert> unacknowledged = securityAlertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc();

        // Assert
        assertThat(unacknowledged).hasSize(2);
        assertThat(unacknowledged).allMatch(a -> !a.isAcknowledged());
    }

    @Test
    @DisplayName("Should find alerts by zone")
    void testFindByGeofenceZoneId() {
        // Arrange
        GeofenceZone zone2 = GeofenceZone.builder()
                .zoneCode("ZONE_2")
                .zoneName("Zone 2")
                .triggerClass("car")
                .confidenceThreshold(0.80)
                .enabled(true)
                .severity("MEDIUM")
                .alertMessage("Car detected")
                .build();
        geofenceZoneRepository.save(zone2);

        SecurityAlert alert1 = createAlert(testZone, testDetection, false, null);
        SecurityAlert alert2 = createAlert(zone2, testDetection, false, null);

        securityAlertRepository.saveAll(List.of(alert1, alert2));

        // Act
        List<SecurityAlert> zoneAlerts = securityAlertRepository
                .findByGeofenceZoneIdOrderByCreatedAtDesc(testZone.getId());

        // Assert
        assertThat(zoneAlerts).hasSize(1);
        assertThat(zoneAlerts.get(0).getGeofenceZoneId()).isEqualTo(testZone.getId());
    }

    @Test
    @DisplayName("Should find alerts by severity")
    void testFindBySeverity() {
        // Arrange
        SecurityAlert critical = createAlert(testZone, testDetection, false, null);
        critical.setSeverity("CRITICAL");

        SecurityAlert high = createAlert(testZone, testDetection, false, null);
        high.setSeverity("HIGH");

        SecurityAlert medium = createAlert(testZone, testDetection, false, null);
        medium.setSeverity("MEDIUM");

        securityAlertRepository.saveAll(List.of(critical, high, medium));

        // Act
        List<SecurityAlert> criticalAlerts = securityAlertRepository.findBySeverityOrderByCreatedAtDesc("CRITICAL");

        // Assert
        assertThat(criticalAlerts).hasSize(1);
        assertThat(criticalAlerts.get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Should find alerts by time range")
    void testFindByTimeRange() {
        // Arrange
        SecurityAlert alert1 = createAlert(testZone, testDetection, false, null);
        SecurityAlert alert2 = createAlert(testZone, testDetection, false, null);

        securityAlertRepository.saveAll(List.of(alert1, alert2));

        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // Act
        List<SecurityAlert> alertsInRange = securityAlertRepository.findByTimeRange(start, end);

        // Assert
        assertThat(alertsInRange).hasSize(2);
        assertThat(alertsInRange).allMatch(a -> !a.getCreatedAt().isBefore(start) && !a.getCreatedAt().isAfter(end));
    }

    @Test
    @DisplayName("Should count unacknowledged alerts by zone")
    void testCountUnacknowledgedByZone() {
        // Arrange
        createAlert(testZone, testDetection, false, null);
        createAlert(testZone, testDetection, false, null);
        createAlert(testZone, testDetection, true, "admin");

        // Act
        long count = securityAlertRepository.countByGeofenceZoneIdAndAcknowledgedFalse(testZone.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count total alerts by severity")
    void testCountBySeverity() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            SecurityAlert alert = createAlert(testZone, testDetection, false, null);
            alert.setSeverity("CRITICAL");
            securityAlertRepository.save(alert);
        }

        // Act
        long count = securityAlertRepository.countBySeverity("CRITICAL");

        // Assert
        assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should find paginated unacknowledged alerts")
    void testFindUnacknowledgedPaginated() {
        // Arrange
        for (int i = 0; i < 10; i++) {
            createAlert(testZone, testDetection, false, null);
        }

        // Act
        var page = securityAlertRepository.findByAcknowledgedFalse(PageRequest.of(0, 5));

        // Assert
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should delete alert")
    void testDeleteAlert() {
        // Arrange
        SecurityAlert alert = createAlert(testZone, testDetection, false, null);
        securityAlertRepository.save(alert);

        assertThat(securityAlertRepository.findById(alert.getId())).isPresent();

        // Act
        securityAlertRepository.deleteById(alert.getId());

        // Assert
        assertThat(securityAlertRepository.findById(alert.getId())).isEmpty();
    }

    // Helper method
    private SecurityAlert createAlert(GeofenceZone zone, DetectionEvent detection, 
                                     boolean acknowledged, String acknowledgedBy) {
        SecurityAlert alert = SecurityAlert.builder()
                .geofenceZoneId(zone.getId())
                .detectionEventId(detection.getId())
                .severity("HIGH")
                .alertMessage("Test alert")
                .acknowledged(acknowledged)
                .acknowledgedBy(acknowledgedBy)
                .build();
        return securityAlertRepository.save(alert);
    }
}
