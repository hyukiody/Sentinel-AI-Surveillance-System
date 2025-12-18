package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.SecurityAlert;
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
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, UUID> {

    /**
     * Find all unacknowledged alerts, ordered by severity and time.
     */
    List<SecurityAlert> findByAcknowledgedFalseOrderByCreatedAtDesc();

    /**
     * Paginated view for unacknowledged alerts.
     */
    Page<SecurityAlert> findByAcknowledgedFalse(Pageable pageable);

    /**
     * Find all alerts for a specific geofence zone.
     */
    List<SecurityAlert> findByGeofenceZoneIdOrderByCreatedAtDesc(UUID geofenceZoneId);

    /**
     * Find alerts by severity level.
     */
    List<SecurityAlert> findBySeverityOrderByCreatedAtDesc(String severity);

    /**
     * Find all alerts within a time range.
     */
    @Query("SELECT a FROM SecurityAlert a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<SecurityAlert> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count unacknowledged alerts by zone.
     */
    long countByGeofenceZoneIdAndAcknowledgedFalse(UUID geofenceZoneId);

    /**
     * Count total alerts by severity.
     */
    long countBySeverity(String severity);
}
