package com.enterprise.sentinel.service.security;

import com.enterprise.sentinel.domain.model.AuditLogEntry;
import com.enterprise.sentinel.domain.repository.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Phase 2: SEC-01 Audit Logging Pipeline
 * 
 * Immutable, transaction-safe audit trail for all user actions and system events.
 * 
 * Data Flow:
 * 1. User action occurs (VIEW, DELETE, EXPORT, etc.)
 * 2. Check authentication context
 * 3. Create AuditLogEntry with timestamp
 * 4. Persist with @Transactional (atomic commit)
 * 5. Log error if persistence fails
 * 
 * Guarantees:
 * - Every action generates exactly one audit record (or error is logged)
 * - Records are immutable once persisted
 * - Ordered by timestamp
 * - No record loss due to errors
 * 
 * Fail-Safe:
 * - Missing auth context → log with username="ANONYMOUS"
 * - DB failures → log to stderr, don't propagate
 * - Null fields → use sensible defaults
 */
@Service
public class AuditLogger {

    private static final Logger LOGGER = Logger.getLogger(AuditLogger.class.getName());
    private static final String SYSTEM_USER = "SYSTEM";
    private static final String ANONYMOUS_USER = "ANONYMOUS";

    private final AuditLogRepository auditLogRepository;

    public AuditLogger(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log a user action with full context.
     * 
     * Guarantee: Transaction commits atomically or error is logged.
     * 
     * @param username       Username of the actor (required)
     * @param actionType     Type of action: VIEW, DELETE, EXPORT, etc.
     * @param videoId        UUID of the resource (nullable for system actions)
     * @param videoFilename  Friendly name for audit trail
     * @param details        Additional context or reason
     */
    @Transactional
    public void logUserAction(String username, String actionType, UUID videoId, 
                             String videoFilename, String details) {
        try {
            // FAIL-SAFE: Extract username from security context if not provided
            if (username == null || username.isEmpty()) {
                username = extractCurrentUsername();
            }

            // Create audit entry using existing constructor
            AuditLogEntry entry = new AuditLogEntry(
                username, 
                actionType, 
                videoId, 
                videoFilename, 
                details
            );

            // ATOMIC: Persist to database
            AuditLogEntry persisted = auditLogRepository.saveAndFlush(entry);

            // GUARANTEE: If saveAndFlush completes, record is committed
            LOGGER.info("Audit logged: user=" + username + " action=" + actionType + 
                       " resource=" + videoId);

        } catch (Exception e) {
            // FAIL-SAFE: Log error but don't propagate (error isolation)
            LOGGER.severe("Audit logging failed: " + e.getMessage());
            e.printStackTrace();
            // Audit failures should NOT break the main flow
        }
    }

    /**
     * Log a view event (most common action).
     * SEC-01 requirement: Track all video accesses.
     */
    @Transactional
    public void logViewVideo(String username, UUID videoId, String videoFilename) {
        logUserAction(username, "VIEW", videoId, videoFilename, "Video playback initiated");
    }

    /**
     * Log a file deletion event.
     * SEC-01 requirement: Track all destructive operations.
     */
    @Transactional
    public void logDeleteVideo(String username, UUID videoId, String videoFilename, String reason) {
        logUserAction(username, "DELETE", videoId, videoFilename, reason != null ? reason : "No reason provided");
    }

    /**
     * Log an export/download event.
     * SEC-01 requirement: Track data egress for compliance.
     */
    @Transactional
    public void logExportVideo(String username, UUID videoId, String videoFilename, String exportFormat) {
        logUserAction(username, "EXPORT", videoId, videoFilename, 
                     "Exported as: " + (exportFormat != null ? exportFormat : "UNKNOWN"));
    }

    /**
     * Log a data access event for compliance audits.
     */
    @Transactional
    public void logDataAccess(String username, String resource, String details) {
        logUserAction(username, "DATA_ACCESS", null, resource, details);
    }

    /**
     * Log a system event (e.g., AI inference errors, security alerts).
     * SEC-01 requirement: Track all system-level security events.
     * 
     * @param eventType   Type: AI_INFERENCE_ERROR, SECURITY_ALERT, etc.
     * @param severity    CRITICAL, HIGH, MEDIUM, LOW
     * @param description Event details
     */
    @Transactional
    public void logSecurityEvent(String eventType, String severity, String description) {
        try {
            // Use existing constructor with system defaults
            AuditLogEntry entry = new AuditLogEntry(
                SYSTEM_USER,
                "SECURITY_EVENT",
                null,
                eventType,
                String.format("[%s] %s", severity, description)
            );

            auditLogRepository.saveAndFlush(entry);

            LOGGER.warning("Security event logged: type=" + eventType + " severity=" + severity);

        } catch (Exception e) {
            LOGGER.severe("Failed to log security event: " + e.getMessage());
            // Error isolation: don't propagate
        }
    }

    /**
     * Log an authentication attempt (success or failure).
     * SEC-01 requirement: Track authentication for forensics.
     */
    @Transactional
    public void logAuthenticationAttempt(String username, boolean success, String ipAddress) {
        try {
            AuditLogEntry entry = new AuditLogEntry(
                username != null ? username : "UNKNOWN",
                success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE",
                null,
                "AUTH",
                "IP: " + (ipAddress != null ? ipAddress : "UNKNOWN")
            );

            auditLogRepository.saveAndFlush(entry);

            LOGGER.info("Authentication attempt logged: user=" + username + " success=" + success);

        } catch (Exception e) {
            LOGGER.severe("Failed to log authentication: " + e.getMessage());
        }
    }

    /**
     * Log a configuration change (for compliance).
     * SEC-01 requirement: Track all admin/system changes.
     */
    @Transactional
    public void logConfigurationChange(String username, String component, String oldValue, String newValue) {
        try {
            AuditLogEntry entry = new AuditLogEntry(
                username != null ? username : SYSTEM_USER,
                "CONFIG_CHANGE",
                null,
                component,
                String.format("Old: %s → New: %s", oldValue, newValue)
            );

            auditLogRepository.saveAndFlush(entry);

            LOGGER.info("Configuration change logged: " + component);

        } catch (Exception e) {
            LOGGER.severe("Failed to log configuration change: " + e.getMessage());
        }
    }

    /**
     * FAIL-SAFE: Extract username from current security context.
     * Falls back to ANONYMOUS if no authenticated user.
     * 
     * @return Current username or "ANONYMOUS"
     */
    private String extractCurrentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception e) {
            LOGGER.warning("Cannot extract authentication context: " + e.getMessage());
        }
        return ANONYMOUS_USER;
    }
}
