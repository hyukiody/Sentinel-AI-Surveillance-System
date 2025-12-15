package com.enterprise.sentinel.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "detection_events", indexes = {
    // Index for fast timeline queries
    @Index(name = "idx_event_video_time", columnList = "video_id, timestamp_ms") 
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "timestamp_ms", nullable = false)
    private Long timestampMs;

    // DYNAMIC SCHEMA: Stores the full YOLOv8 output
    // Structure: { "class": "person", "conf": 0.95, "bbox": [x, y, w, h] }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inference_data", columnDefinition = "jsonb")
    private Map<String, Object> inferenceData;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}