package com.enterprise.sentinel.service.security;

import com.enterprise.sentinel.domain.model.AuditLogEntry;
import com.enterprise.sentinel.domain.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * SEC-01: Enterprise audit logging service.
 * Records immutable records of all user interactions with video files.
 * Enables compliance, accountability, and forensic analysis.
 */
@Service
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;

    public AuditLogger(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log a user action on a video file.
     * Thread-safe and designed for high-volume logging.
     *
     * @param username       Username of the actor
     * @param actionType     Type of action (e.g., "VIEW", "EXPORT", "DELETE")
     * @param videoId        UUID of the video file (nullable for system actions)
     * @param videoFilename  Friendly filename for audit trail readability
     * @param details        Additional context or reason for the action
     */
    public void logUserAction(String username, String actionType, UUID videoId, String videoFilename, String details) {
        AuditLogEntry entry = new AuditLogEntry(username, actionType, videoId, videoFilename, details);
        auditLogRepository.save(entry);
        System.out.println("📋 Audit: " + entry);
    }

    /**
     * Log a view event (most common action).
     */
    public void logViewVideo(String username, UUID videoId, String videoFilename) {
        logUserAction(username, "VIEW", videoId, videoFilename, "Video playback initiated");
    }

    /**
     * Log a file deletion event.
     */
    public void logDeleteVideo(String username, UUID videoId, String videoFilename, String reason) {
        logUserAction(username, "DELETE", videoId, videoFilename, reason);
    }

    /**
     * Log an export/download event.
     */
    public void logExportVideo(String username, UUID videoId, String videoFilename, String exportFormat) {
        logUserAction(username, "EXPORT", videoId, videoFilename, "Exported as: " + exportFormat);
    }

    /**
     * Log a data access event for compliance.
     */
    public void logDataAccess(String username, String resource, String details) {
        logUserAction(username, "DATA_ACCESS", null, resource, details);
    }
}
