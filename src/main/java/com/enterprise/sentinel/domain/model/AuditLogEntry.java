package com.enterprise.sentinel.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit log record capturing user interactions with video files.
 * SEC-01: Provides compliance and accountability for enterprise deployments.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_time", columnList = "username, created_at DESC"),
    @Index(name = "idx_audit_video_id", columnList = "video_id"),
    @Index(name = "idx_audit_action_type", columnList = "action_type")
})
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String username; // Who accessed the file

    @Column(nullable = false)
    private String actionType; // What action was performed (VIEW, EXPORT, DELETE, etc.)

    @Column
    private UUID videoId; // Which video file was accessed

    @Column
    private String videoFilename; // Friendly name for audit trail

    @Column(length = 1024)
    private String details; // Additional context (IP, reason, etc.)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // When the action occurred

    // ===== Constructors =====
    
    public AuditLogEntry() {
    }

    public AuditLogEntry(String username, String actionType, UUID videoId, String videoFilename, String details) {
        this.username = username;
        this.actionType = actionType;
        this.videoId = videoId;
        this.videoFilename = videoFilename;
        this.details = details;
    }

    // ===== Getters (Immutable Pattern) =====

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getActionType() {
        return actionType;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public String getVideoFilename() {
        return videoFilename;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "AuditLogEntry{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", actionType='" + actionType + '\'' +
                ", videoId=" + videoId +
                ", videoFilename='" + videoFilename + '\'' +
                ", details='" + details + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
