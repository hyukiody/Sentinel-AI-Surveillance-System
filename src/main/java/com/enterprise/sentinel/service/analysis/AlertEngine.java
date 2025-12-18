package com.enterprise.sentinel.service.analysis;

import com.enterprise.sentinel.domain.model.DetectionEvent;
import com.enterprise.sentinel.domain.model.GeofenceZone;
import com.enterprise.sentinel.domain.model.SecurityAlert;
import com.enterprise.sentinel.domain.repository.GeofenceZoneRepository;
import com.enterprise.sentinel.domain.repository.SecurityAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * AlertEngine evaluates detected objects against geofence zones and alert rules.
 * Implements non-blocking alert dispatch with event publishing for decoupled notification handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertEngine {

    private final GeofenceZoneRepository geofenceZoneRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Process a detection event and evaluate against all enabled geofence zones.
     * Creates SecurityAlert if zone trigger conditions are met.
     *
     * @param detectionEvent Detection from ObjectDetectionService
     * @return true if alert was created, false otherwise
     */
    public boolean processDetection(DetectionEvent detectionEvent) {
        if (detectionEvent == null || detectionEvent.getInferenceData() == null) {
            return false;
        }

        List<GeofenceZone> activeZones = geofenceZoneRepository.findByEnabledTrueOrderByZoneName();
        boolean alertCreated = false;

        for (GeofenceZone zone : activeZones) {
            if (shouldTriggerAlert(zone, detectionEvent)) {
                SecurityAlert alert = createAlert(zone, detectionEvent);
                securityAlertRepository.save(alert);
                
                log.info("Alert triggered: zone={}, detectedClass={}, severity={}, alertId={}",
                        zone.getZoneCode(), detectionEvent.getDetectedClass(), 
                        zone.getSeverity(), alert.getId());

                // Publish event for async notification handling
                eventPublisher.publishEvent(new SecurityAlertEvent(alert));
                alertCreated = true;
            }
        }

        return alertCreated;
    }

    /**
     * Evaluate if detection meets zone trigger criteria.
     * Checks:
     * 1. Zone is enabled
     * 2. Detected class matches zone trigger class
     * 3. Detection confidence meets or exceeds zone threshold
     */
    private boolean shouldTriggerAlert(GeofenceZone zone, DetectionEvent detectionEvent) {
        if (!zone.isEnabled()) {
            return false;
        }

        String detectedClass = detectionEvent.getDetectedClass();
        double confidence = detectionEvent.getConfidence();

        return zone.shouldTriggerAlert(detectedClass, confidence);
    }

    /**
     * Create a SecurityAlert entity from zone and detection.
     */
    private SecurityAlert createAlert(GeofenceZone zone, DetectionEvent detectionEvent) {
        return SecurityAlert.builder()
                .geofenceZoneId(zone.getId())
                .detectionEventId(detectionEvent.getId())
                .severity(zone.getSeverity())
                .alertMessage(zone.getAlertMessage())
                .acknowledged(false)
                .build();
    }

    /**
     * Acknowledge an alert (user action from UI).
     * Called when operator views and acknowledges an alert.
     *
     * @param alertId ID of alert to acknowledge
     * @param username Username of acknowledging operator
     */
    @Transactional
    public void acknowledgeAlert(String alertId, String username) {
        securityAlertRepository.findById(java.util.UUID.fromString(alertId))
                .ifPresentOrElse(
                        alert -> {
                            alert.acknowledge(username);
                            securityAlertRepository.save(alert);
                            log.info("Alert acknowledged: alertId={}, username={}", alertId, username);
                        },
                        () -> log.warn("Alert not found: alertId={}", alertId)
                );
    }

    /**
     * Get count of unacknowledged alerts for a zone.
     * Used for real-time UI indicators.
     */
    public long getUnacknowledgedAlertCount(String zoneCode) {
        return geofenceZoneRepository.findByZoneCode(zoneCode)
                .map(zone -> securityAlertRepository.countByGeofenceZoneIdAndAcknowledgedFalse(zone.getId()))
                .orElse(0L);
    }

    /**
     * Get critical alerts requiring immediate attention.
     */
    public List<SecurityAlert> getCriticalUnacknowledgedAlerts() {
        return securityAlertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(alert -> "CRITICAL".equalsIgnoreCase(alert.getSeverity()))
                .toList();
    }
}
