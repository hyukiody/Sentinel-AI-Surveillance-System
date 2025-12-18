package com.enterprise.sentinel.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phase 3: Geofence Zone - Defines restricted areas with compliance policies.
 * A zone represents a logical region (e.g., "Restricted Area B", "Warehouse Entry")
 * where specific detection classes trigger alerts or require compliance checks.
 */
@Entity
@Table(name = "geofence_zones", indexes = {
    @Index(name = "idx_zone_code", columnList = "zone_code", unique = true),
    @Index(name = "idx_zone_enabled", columnList = "enabled")
})
public class GeofenceZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String zoneCode; // e.g., "ZONE_B_WAREHOUSE"

    @Column(nullable = false)
    private String zoneName; // Human-readable name

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String triggerClass; // e.g., "person", "weapon", "unknown_individual"

    @Column(nullable = false)
    private double confidenceThreshold; // Minimum confidence to trigger (0.0-1.0)

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(length = 500)
    private String alertMessage; // Custom alert message template

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== Constructors =====

    public GeofenceZone() {
    }

    public GeofenceZone(String zoneCode, String zoneName, String triggerClass, double confidenceThreshold, String severity) {
        this.zoneCode = zoneCode;
        this.zoneName = zoneName;
        this.triggerClass = triggerClass;
        this.confidenceThreshold = confidenceThreshold;
        this.severity = severity;
    }

    // ===== Getters =====

    public UUID getId() {
        return id;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getDescription() {
        return description;
    }

    public String getTriggerClass() {
        return triggerClass;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSeverity() {
        return severity;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ===== Business Logic =====

    /**
     * Determines if a detection should trigger an alert in this zone.
     */
    public boolean shouldTriggerAlert(String detectedClass, double confidence) {
        if (!enabled) {
            return false;
        }
        return detectedClass.equalsIgnoreCase(triggerClass) && confidence >= confidenceThreshold;
    }

    @Override
    public String toString() {
        return "GeofenceZone{" +
                "id=" + id +
                ", zoneCode='" + zoneCode + '\'' +
                ", zoneName='" + zoneName + '\'' +
                ", triggerClass='" + triggerClass + '\'' +
                ", confidenceThreshold=" + confidenceThreshold +
                ", severity='" + severity + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
