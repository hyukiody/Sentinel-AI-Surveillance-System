package com.enterprise.sentinel.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phase 3: Security Alert - Immutable record of triggered security alerts.
 * Captures when and why an alert was raised, linking to the detection, zone, and audit context.
 */
@Entity
@Table(name = "security_alerts", indexes = {
    @Index(name = "idx_alert_zone_time", columnList = "geofence_zone_id, created_at DESC"),
    @Index(name = "idx_alert_severity", columnList = "severity"),
    @Index(name = "idx_alert_acknowledged", columnList = "acknowledged")
})
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID geofenceZoneId; // Reference to triggering zone

    @Column(nullable = false)
    private UUID detectionEventId; // Reference to detection that triggered alert

    @Column(nullable = false)
    private String detectedClass; // What was detected (e.g., "weapon", "unknown_individual")

    @Column(nullable = false)
    private double confidence; // Confidence score of detection

    @Column(nullable = false)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(length = 1000)
    private String alertMessage; // Full alert message

    @Column(nullable = false)
    private boolean acknowledged = false;

    @Column
    private String acknowledgedBy; // User who acknowledged the alert

    @Column
    private LocalDateTime acknowledgedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== Constructors =====

    public SecurityAlert() {
    }

    public SecurityAlert(UUID geofenceZoneId, UUID detectionEventId, String detectedClass, 
                        double confidence, String severity, String alertMessage) {
        this.geofenceZoneId = geofenceZoneId;
        this.detectionEventId = detectionEventId;
        this.detectedClass = detectedClass;
        this.confidence = confidence;
        this.severity = severity;
        this.alertMessage = alertMessage;
    }

    // ===== Getters =====

    public UUID getId() {
        return id;
    }

    public UUID getGeofenceZoneId() {
        return geofenceZoneId;
    }

    public UUID getDetectionEventId() {
        return detectionEventId;
    }

    public String getDetectedClass() {
        return detectedClass;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSeverity() {
        return severity;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ===== Business Logic =====

    /**
     * Mark alert as acknowledged by a user.
     */
    public void acknowledge(String username) {
        this.acknowledged = true;
        this.acknowledgedBy = username;
        this.acknowledgedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SecurityAlert{" +
                "id=" + id +
                ", geofenceZoneId=" + geofenceZoneId +
                ", detectedClass='" + detectedClass + '\'' +
                ", confidence=" + confidence +
                ", severity='" + severity + '\'' +
                ", acknowledged=" + acknowledged +
                ", createdAt=" + createdAt +
                '}';
    }
}
