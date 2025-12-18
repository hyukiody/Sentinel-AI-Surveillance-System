package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    /**
     * Find all audit logs for a specific user.
     */
    List<AuditLogEntry> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * Find all audit logs for a specific video file.
     */
    List<AuditLogEntry> findByVideoIdOrderByCreatedAtDesc(UUID videoId);

    /**
     * Find all audit logs of a specific action type (e.g., VIEW, EXPORT).
     */
    List<AuditLogEntry> findByActionTypeOrderByCreatedAtDesc(String actionType);

    /**
     * Paginated query for audit trail UI (e.g., admin dashboard).
     */
    @Query("SELECT a FROM AuditLogEntry a WHERE a.username = :username ORDER BY a.createdAt DESC")
    Page<AuditLogEntry> findByUsernamePaginated(@Param("username") String username, Pageable pageable);

    /**
     * Find audit entries within a date range (for compliance reports).
     */
    @Query("SELECT a FROM AuditLogEntry a WHERE a.createdAt BETWEEN :startTime AND :endTime ORDER BY a.createdAt DESC")
    List<AuditLogEntry> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Count total audit entries for a specific user.
     */
    long countByUsername(String username);

    /**
     * Count total audit entries for a specific action type.
     */
    long countByActionType(String actionType);
}
