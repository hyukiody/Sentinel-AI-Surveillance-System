package com.enterprise.sentinel.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "detection_events", indexes = {
    @Index(name = "idx_event_video_time", columnList = "video_id, timestamp_ms"),
    @Index(name = "idx_event_class", columnList = "detected_class"),
    @Index(name = "idx_event_confidence", columnList = "confidence")
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
    @JoinColumn(name = "video_id", nullable = true)
    private Video video;

    @Column(name = "video_id", insertable = false, updatable = false)
    private UUID videoId;

    @Column(name = "timestamp_ms")
    private Long timestampMs;

    @Column(name = "detected_class", nullable = false)
    private String detectedClass;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "bounding_box")
    private String boundingBox;

    // DYNAMIC SCHEMA: Stores the full YOLOv8 output
    // Structure: { "class": "person", "conf": 0.95, "bbox": [x, y, w, h], ... }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inference_data", columnDefinition = "jsonb")
    private Map<String, Object> inferenceData;

    @CreationTimestamp
    private LocalDateTime createdAt;
}