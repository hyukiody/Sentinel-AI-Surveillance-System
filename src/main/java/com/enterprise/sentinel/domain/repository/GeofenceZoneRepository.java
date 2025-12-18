package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeofenceZoneRepository extends JpaRepository<GeofenceZone, UUID> {

    /**
     * Find zone by unique code.
     */
    Optional<GeofenceZone> findByZoneCode(String zoneCode);

    /**
     * Find all enabled zones.
     */
    List<GeofenceZone> findByEnabledTrueOrderByZoneName();

    /**
     * Find all zones that trigger on a specific class.
     */
    List<GeofenceZone> findByTriggerClassAndEnabledTrue(String triggerClass);

    /**
     * Find all zones by severity level.
     */
    List<GeofenceZone> findBySeverityOrderByUpdatedAtDesc(String severity);

    /**
     * Count total enabled zones.
     */
    long countByEnabledTrue();
}
