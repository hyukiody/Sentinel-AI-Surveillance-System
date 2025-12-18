package com.enterprise.sentinel.service.analysis;

import com.enterprise.sentinel.domain.model.SecurityAlert;
import com.enterprise.sentinel.service.security.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AlertNotificationService listens for SecurityAlertEvents and handles notifications.
 * Maintains real-time alert queue for UI consumption.
 * Integrates with audit logging for compliance tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final AuditLogger auditLogger;

    /**
     * In-memory queue for real-time alerts (max 1000 recent alerts).
     * UI components can poll this queue for live alert display.
     */
    private final ConcurrentLinkedQueue<SecurityAlert> alertQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_ALERT_QUEUE_SIZE = 1000;

    /**
     * Listen for SecurityAlertEvent and dispatch notifications.
     * Triggered when AlertEngine publishes a new alert.
     */
    @EventListener
    public void onSecurityAlert(SecurityAlertEvent event) {
        SecurityAlert alert = event.getAlert();
        
        // Add to real-time queue for UI
        alertQueue.offer(alert);
        if (alertQueue.size() > MAX_ALERT_QUEUE_SIZE) {
            alertQueue.poll(); // Remove oldest if queue exceeds max
        }

        // Log alert creation for audit trail
        auditLogger.logDataAccess(
                "SYSTEM",
                String.format("Security alert triggered: %s (severity: %s)", 
                        alert.getAlertMessage(), alert.getSeverity()),
                alert.getGeofenceZoneId().toString()
        );

        log.info("Alert notification dispatched: alertId={}, severity={}, queue_size={}", 
                alert.getId(), alert.getSeverity(), alertQueue.size());

        // Future: Integrate with external notification services
        // - Send email to security team for CRITICAL alerts
        // - Send SMS to on-call personnel
        // - Integrate with SIEM (Splunk, ELK, etc.)
        // - Send webhook to external systems
    }

    /**
     * Get all recent alerts (last N from queue).
     * Used by UI dashboard to display alert history.
     */
    public java.util.List<SecurityAlert> getRecentAlerts(int count) {
        return alertQueue.stream()
                .limit(count)
                .toList();
    }

    /**
     * Get current queue size (real-time indicator).
     */
    public int getUnacknowledgedAlertCount() {
        return (int) alertQueue.stream()
                .filter(alert -> !alert.isAcknowledged())
                .count();
    }

    /**
     * Clear alerts from queue (admin operation).
     * Should be audit logged separately.
     */
    public void clearAlertQueue() {
        int clearedCount = alertQueue.size();
        alertQueue.clear();
        log.warn("Alert queue cleared: count={}", clearedCount);
        auditLogger.logDataAccess("SYSTEM", "Alert queue cleared", String.valueOf(clearedCount));
    }
}
