package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.DetectionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface DetectionEventRepository extends JpaRepository<DetectionEvent, UUID> {
    
    List<DetectionEvent> findByVideoIdOrderByTimestampMsAsc(UUID videoId);

    // Native Query example for JSONB filtering (Find all 'person' detections)
    @Query(value = "SELECT * FROM detection_events WHERE video_id = :vid " +
           "AND inference_data ->> 'class' = :objLabel", nativeQuery = true)
    List<DetectionEvent> findObjectsByClass(@Param("vid") UUID videoId, 
                                            @Param("objLabel") String label);
}