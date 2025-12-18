package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/test_db",
    "spring.datasource.username=postgres",
    "spring.datasource.password=password"
})
class AuditLogRepositoryIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void shouldSaveAndRetrieveAuditLogEntry() {
        UUID videoId = UUID.randomUUID();
        AuditLogEntry entry = new AuditLogEntry(
            "testuser",
            "VIEW",
            videoId,
            "test-video.mp4",
            "Integration test"
        );

        AuditLogEntry saved = auditLogRepository.save(entry);

        assertNotNull(saved.getId());
        assertEquals("testuser", saved.getUsername());
        assertEquals("VIEW", saved.getActionType());
    }

    @Test
    void shouldFindAuditLogsByUsername() {
        String username = "auditor";
        UUID videoId = UUID.randomUUID();
        
        auditLogRepository.save(new AuditLogEntry(username, "VIEW", videoId, "video1.mp4", ""));
        auditLogRepository.save(new AuditLogEntry(username, "EXPORT", videoId, "video1.mp4", ""));

        List<AuditLogEntry> entries = auditLogRepository.findByUsernameOrderByCreatedAtDesc(username);

        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(e -> e.getUsername().equals(username)));
    }

    @Test
    void shouldFindAuditLogsByVideoId() {
        UUID videoId = UUID.randomUUID();
        
        auditLogRepository.save(new AuditLogEntry("user1", "VIEW", videoId, "video.mp4", ""));
        auditLogRepository.save(new AuditLogEntry("user2", "VIEW", videoId, "video.mp4", ""));

        List<AuditLogEntry> entries = auditLogRepository.findByVideoIdOrderByCreatedAtDesc(videoId);

        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(e -> e.getVideoId().equals(videoId)));
    }

    @Test
    void shouldFindAuditLogsByActionType() {
        UUID videoId = UUID.randomUUID();
        
        auditLogRepository.save(new AuditLogEntry("user1", "VIEW", videoId, "video.mp4", ""));
        auditLogRepository.save(new AuditLogEntry("user2", "VIEW", videoId, "video.mp4", ""));
        auditLogRepository.save(new AuditLogEntry("user3", "EXPORT", videoId, "video.mp4", ""));

        List<AuditLogEntry> viewEntries = auditLogRepository.findByActionTypeOrderByCreatedAtDesc("VIEW");

        assertEquals(2, viewEntries.size());
        assertTrue(viewEntries.stream().allMatch(e -> e.getActionType().equals("VIEW")));
    }

    @Test
    void shouldCountAuditLogsByUsername() {
        String username = "analyst";
        UUID videoId = UUID.randomUUID();
        
        auditLogRepository.save(new AuditLogEntry(username, "VIEW", videoId, "video1.mp4", ""));
        auditLogRepository.save(new AuditLogEntry(username, "EXPORT", videoId, "video2.mp4", ""));

        long count = auditLogRepository.countByUsername(username);

        assertEquals(2, count);
    }
}
