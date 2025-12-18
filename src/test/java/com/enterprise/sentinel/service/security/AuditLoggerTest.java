package com.enterprise.sentinel.service.security;

import static org.junit.jupiter.api.Assertions.*;

import com.enterprise.sentinel.domain.model.AuditLogEntry;
import com.enterprise.sentinel.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;

class AuditLoggerTest {

    private AuditLogger auditLogger;

    @Mock
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditLogger = new AuditLogger(auditLogRepository);
    }

    @Test
    void shouldLogUserAction() {
        UUID videoId = UUID.randomUUID();
        String username = "admin";
        String action = "VIEW";
        String filename = "test-video.mp4";
        String details = "Test details";

        auditLogger.logUserAction(username, action, videoId, filename, details);

        verify(auditLogRepository, times(1)).save(any(AuditLogEntry.class));
    }

    @Test
    void shouldLogViewVideo() {
        UUID videoId = UUID.randomUUID();
        
        auditLogger.logViewVideo("operator", videoId, "surveillance.mp4");

        verify(auditLogRepository, times(1)).save(any(AuditLogEntry.class));
    }

    @Test
    void shouldLogDeleteVideo() {
        UUID videoId = UUID.randomUUID();
        
        auditLogger.logDeleteVideo("admin", videoId, "archive.mp4", "Compliance retention policy");

        verify(auditLogRepository, times(1)).save(any(AuditLogEntry.class));
    }

    @Test
    void shouldLogExportVideo() {
        UUID videoId = UUID.randomUUID();
        
        auditLogger.logExportVideo("analyst", videoId, "evidence.mp4", "MP4");

        verify(auditLogRepository, times(1)).save(any(AuditLogEntry.class));
    }

    @Test
    void shouldLogDataAccess() {
        auditLogger.logDataAccess("auditor", "AUDIT_LOGS", "Generated compliance report");

        verify(auditLogRepository, times(1)).save(any(AuditLogEntry.class));
    }
}
