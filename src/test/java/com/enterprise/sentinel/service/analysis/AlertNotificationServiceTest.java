package com.enterprise.sentinel.service.analysis;

import com.enterprise.sentinel.domain.model.SecurityAlert;
import com.enterprise.sentinel.service.security.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;

@DisplayName("AlertNotificationService Unit Tests")
class AlertNotificationServiceTest {

    @Mock
    private AuditLogger auditLogger;

    private AlertNotificationService alertNotificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        alertNotificationService = new AlertNotificationService(auditLogger);
    }

    @Test
    @DisplayName("Should add alert to queue on event")
    void testOnSecurityAlert_AddsToQueue() {
        // Arrange
        SecurityAlert alert = SecurityAlert.builder()
                .id(UUID.randomUUID())
                .severity("HIGH")
                .alertMessage("Test Alert")
                .geofenceZoneId(UUID.randomUUID())
                .acknowledged(false)
                .build();

        SecurityAlertEvent event = new SecurityAlertEvent(alert);

        // Act
        alertNotificationService.onSecurityAlert(event);

        // Assert
        List<SecurityAlert> recentAlerts = alertNotificationService.getRecentAlerts(10);
        assertThat(recentAlerts).contains(alert);
        verify(auditLogger).logDataAccess("SYSTEM", 
                "Security alert triggered: Test Alert (severity: HIGH)",
                alert.getGeofenceZoneId().toString());
    }

    @Test
    @DisplayName("Should maintain max queue size")
    void testOnSecurityAlert_MaxQueueSize() {
        // Arrange - Create 1100 alerts (exceeds max of 1000)
        for (int i = 0; i < 1100; i++) {
            SecurityAlert alert = SecurityAlert.builder()
                    .id(UUID.randomUUID())
                    .severity("MEDIUM")
                    .alertMessage("Alert " + i)
                    .geofenceZoneId(UUID.randomUUID())
                    .acknowledged(false)
                    .build();
            alertNotificationService.onSecurityAlert(new SecurityAlertEvent(alert));
        }

        // Act & Assert
        int queueSize = alertNotificationService.getRecentAlerts(5000).size();
        assertThat(queueSize).isLessThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("Should count unacknowledged alerts")
    void testGetUnacknowledgedAlertCount() {
        // Arrange
        SecurityAlert unacknowledged1 = SecurityAlert.builder()
                .id(UUID.randomUUID())
                .severity("HIGH")
                .geofenceZoneId(UUID.randomUUID())
                .acknowledged(false)
                .build();

        SecurityAlert unacknowledged2 = SecurityAlert.builder()
                .id(UUID.randomUUID())
                .severity("MEDIUM")
                .geofenceZoneId(UUID.randomUUID())
                .acknowledged(false)
                .build();

        SecurityAlert acknowledged = SecurityAlert.builder()
                .id(UUID.randomUUID())
                .severity("LOW")
                .geofenceZoneId(UUID.randomUUID())
                .acknowledged(true)
                .acknowledgedBy("user1")
                .build();

        alertNotificationService.onSecurityAlert(new SecurityAlertEvent(unacknowledged1));
        alertNotificationService.onSecurityAlert(new SecurityAlertEvent(unacknowledged2));
        alertNotificationService.onSecurityAlert(new SecurityAlertEvent(acknowledged));

        // Act
        int count = alertNotificationService.getUnacknowledgedAlertCount();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should retrieve recent alerts with limit")
    void testGetRecentAlerts_WithLimit() {
        // Arrange
        for (int i = 0; i < 10; i++) {
            SecurityAlert alert = SecurityAlert.builder()
                    .id(UUID.randomUUID())
                    .severity("MEDIUM")
                    .geofenceZoneId(UUID.randomUUID())
                    .acknowledged(false)
                    .build();
            alertNotificationService.onSecurityAlert(new SecurityAlertEvent(alert));
        }

        // Act
        List<SecurityAlert> recent = alertNotificationService.getRecentAlerts(5);

        // Assert
        assertThat(recent).hasSize(5);
    }

    @Test
    @DisplayName("Should clear alert queue")
    void testClearAlertQueue() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            SecurityAlert alert = SecurityAlert.builder()
                    .id(UUID.randomUUID())
                    .severity("MEDIUM")
                    .geofenceZoneId(UUID.randomUUID())
                    .acknowledged(false)
                    .build();
            alertNotificationService.onSecurityAlert(new SecurityAlertEvent(alert));
        }

        assertThat(alertNotificationService.getRecentAlerts(10)).hasSize(5);

        // Act
        alertNotificationService.clearAlertQueue();

        // Assert
        assertThat(alertNotificationService.getRecentAlerts(10)).isEmpty();
        verify(auditLogger).logDataAccess("SYSTEM", "Alert queue cleared", "5");
    }

    @Test
    @DisplayName("Should handle empty queue gracefully")
    void testGetRecentAlerts_EmptyQueue() {
        // Act
        List<SecurityAlert> alerts = alertNotificationService.getRecentAlerts(10);

        // Assert
        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("Should return correct count when limit exceeds queue size")
    void testGetRecentAlerts_LimitExceedsQueueSize() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            SecurityAlert alert = SecurityAlert.builder()
                    .id(UUID.randomUUID())
                    .severity("MEDIUM")
                    .geofenceZoneId(UUID.randomUUID())
                    .acknowledged(false)
                    .build();
            alertNotificationService.onSecurityAlert(new SecurityAlertEvent(alert));
        }

        // Act
        List<SecurityAlert> alerts = alertNotificationService.getRecentAlerts(100);

        // Assert
        assertThat(alerts).hasSize(3);
    }
}
