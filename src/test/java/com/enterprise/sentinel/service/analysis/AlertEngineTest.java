package com.enterprise.sentinel.service.analysis;

import com.enterprise.sentinel.domain.model.DetectionEvent;
import com.enterprise.sentinel.domain.model.GeofenceZone;
import com.enterprise.sentinel.domain.model.SecurityAlert;
import com.enterprise.sentinel.domain.repository.GeofenceZoneRepository;
import com.enterprise.sentinel.domain.repository.SecurityAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AlertEngine Unit Tests")
class AlertEngineTest {

    @Mock
    private GeofenceZoneRepository geofenceZoneRepository;

    @Mock
    private SecurityAlertRepository securityAlertRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AlertEngine alertEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        alertEngine = new AlertEngine(geofenceZoneRepository, securityAlertRepository, eventPublisher);
    }

    @Test
    @DisplayName("Should create alert when detection matches enabled zone")
    void testProcessDetection_MatchesZone() {
        // Arrange
        UUID zoneId = UUID.randomUUID();
        UUID detectionId = UUID.randomUUID();

        GeofenceZone zone = GeofenceZone.builder()
                .id(zoneId)
                .zoneCode("ZONE_A")
                .zoneName("Restricted Area A")
                .triggerClass("person")
                .confidenceThreshold(0.75)
                .enabled(true)
                .severity("HIGH")
                .alertMessage("Unauthorized person detected in Zone A")
                .build();

        DetectionEvent detection = DetectionEvent.builder()
                .id(detectionId)
                .detectedClass("person")
                .confidence(0.95)
                .inferenceData("{}")
                .createdAt(LocalDateTime.now())
                .build();

        when(geofenceZoneRepository.findByEnabledTrueOrderByZoneName())
                .thenReturn(List.of(zone));
        when(securityAlertRepository.save(any(SecurityAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean alertCreated = alertEngine.processDetection(detection);

        // Assert
        assertThat(alertCreated).isTrue();
        verify(securityAlertRepository).save(any(SecurityAlert.class));
        verify(eventPublisher).publishEvent(any(SecurityAlertEvent.class));
    }

    @Test
    @DisplayName("Should not create alert when confidence below threshold")
    void testProcessDetection_LowConfidence() {
        // Arrange
        UUID zoneId = UUID.randomUUID();
        UUID detectionId = UUID.randomUUID();

        GeofenceZone zone = GeofenceZone.builder()
                .id(zoneId)
                .zoneCode("ZONE_A")
                .triggerClass("person")
                .confidenceThreshold(0.80)
                .enabled(true)
                .severity("HIGH")
                .alertMessage("Alert")
                .build();

        DetectionEvent detection = DetectionEvent.builder()
                .id(detectionId)
                .detectedClass("person")
                .confidence(0.65) // Below 0.80 threshold
                .inferenceData("{}")
                .build();

        when(geofenceZoneRepository.findByEnabledTrueOrderByZoneName())
                .thenReturn(List.of(zone));

        // Act
        boolean alertCreated = alertEngine.processDetection(detection);

        // Assert
        assertThat(alertCreated).isFalse();
        verify(securityAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not create alert when class doesn't match")
    void testProcessDetection_ClassMismatch() {
        // Arrange
        UUID zoneId = UUID.randomUUID();

        GeofenceZone zone = GeofenceZone.builder()
                .id(zoneId)
                .triggerClass("person")
                .confidenceThreshold(0.75)
                .enabled(true)
                .build();

        DetectionEvent detection = DetectionEvent.builder()
                .detectedClass("car") // Doesn't match "person"
                .confidence(0.95)
                .inferenceData("{}")
                .build();

        when(geofenceZoneRepository.findByEnabledTrueOrderByZoneName())
                .thenReturn(List.of(zone));

        // Act
        boolean alertCreated = alertEngine.processDetection(detection);

        // Assert
        assertThat(alertCreated).isFalse();
        verify(securityAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not create alert when zone is disabled")
    void testProcessDetection_ZoneDisabled() {
        // Arrange
        GeofenceZone zone = GeofenceZone.builder()
                .id(UUID.randomUUID())
                .triggerClass("person")
                .confidenceThreshold(0.75)
                .enabled(false) // Disabled
                .build();

        DetectionEvent detection = DetectionEvent.builder()
                .detectedClass("person")
                .confidence(0.95)
                .inferenceData("{}")
                .build();

        when(geofenceZoneRepository.findByEnabledTrueOrderByZoneName())
                .thenReturn(List.of(zone)); // Zone returned but ignored due to disabled flag

        // Act
        boolean alertCreated = alertEngine.processDetection(detection);

        // Assert
        assertThat(alertCreated).isFalse();
    }

    @Test
    @DisplayName("Should handle null detection gracefully")
    void testProcessDetection_NullDetection() {
        // Act & Assert
        assertThatNoException().isThrownBy(() -> {
            boolean result = alertEngine.processDetection(null);
            assertThat(result).isFalse();
        });
    }

    @Test
    @DisplayName("Should acknowledge alert with username")
    void testAcknowledgeAlert() {
        // Arrange
        UUID alertId = UUID.randomUUID();
        SecurityAlert alert = SecurityAlert.builder()
                .id(alertId)
                .severity("HIGH")
                .acknowledged(false)
                .build();

        when(securityAlertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));

        // Act
        alertEngine.acknowledgeAlert(alertId.toString(), "admin_user");

        // Assert
        assertThat(alert.isAcknowledged()).isTrue();
        assertThat(alert.getAcknowledgedBy()).isEqualTo("admin_user");
        verify(securityAlertRepository).save(alert);
    }

    @Test
    @DisplayName("Should get unacknowledged alert count for zone")
    void testGetUnacknowledgedAlertCount() {
        // Arrange
        UUID zoneId = UUID.randomUUID();
        GeofenceZone zone = GeofenceZone.builder()
                .id(zoneId)
                .zoneCode("ZONE_A")
                .build();

        when(geofenceZoneRepository.findByZoneCode("ZONE_A"))
                .thenReturn(Optional.of(zone));
        when(securityAlertRepository.countByGeofenceZoneIdAndAcknowledgedFalse(zoneId))
                .thenReturn(5L);

        // Act
        long count = alertEngine.getUnacknowledgedAlertCount("ZONE_A");

        // Assert
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should get critical alerts")
    void testGetCriticalUnacknowledgedAlerts() {
        // Arrange
        SecurityAlert criticalAlert = SecurityAlert.builder()
                .id(UUID.randomUUID())
                .severity("CRITICAL")
                .acknowledged(false)
                .build();

        SecurityAlert highAlert = SecurityAlert.builder()
                .id(UUID.randomUUID())
                .severity("HIGH")
                .acknowledged(false)
                .build();

        when(securityAlertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(criticalAlert, highAlert));

        // Act
        List<SecurityAlert> critical = alertEngine.getCriticalUnacknowledgedAlerts();

        // Assert
        assertThat(critical).hasSize(1);
        assertThat(critical.get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Should create multiple alerts for multiple matching zones")
    void testProcessDetection_MultipleMatchingZones() {
        // Arrange
        UUID detectionId = UUID.randomUUID();

        GeofenceZone zone1 = GeofenceZone.builder()
                .id(UUID.randomUUID())
                .zoneCode("ZONE_A")
                .triggerClass("person")
                .confidenceThreshold(0.75)
                .enabled(true)
                .severity("HIGH")
                .alertMessage("Zone A Alert")
                .build();

        GeofenceZone zone2 = GeofenceZone.builder()
                .id(UUID.randomUUID())
                .zoneCode("ZONE_B")
                .triggerClass("person")
                .confidenceThreshold(0.70)
                .enabled(true)
                .severity("MEDIUM")
                .alertMessage("Zone B Alert")
                .build();

        DetectionEvent detection = DetectionEvent.builder()
                .id(detectionId)
                .detectedClass("person")
                .confidence(0.95)
                .inferenceData("{}")
                .build();

        when(geofenceZoneRepository.findByEnabledTrueOrderByZoneName())
                .thenReturn(List.of(zone1, zone2));
        when(securityAlertRepository.save(any(SecurityAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean alertCreated = alertEngine.processDetection(detection);

        // Assert
        assertThat(alertCreated).isTrue();
        ArgumentCaptor<SecurityAlert> alertCaptor = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(securityAlertRepository, times(2)).save(alertCaptor.capture());
        verify(eventPublisher, times(2)).publishEvent(any(SecurityAlertEvent.class));

        List<SecurityAlert> savedAlerts = alertCaptor.getAllValues();
        assertThat(savedAlerts).hasSize(2);
    }
}
