package com.enterprise.sentinel.domain.model;

import com.enterprise.sentinel.service.security.AttributeEncryptor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String originalFilename;

    // SENSITIVE: Encrypted at rest (Privacy by Design)
    @Convert(converter = AttributeEncryptor.class)
    @Column(nullable = false, length = 1024)
    private String storagePath;

    @Column(nullable = false)
    private String checksum; // SHA-256 for integrity

    @Enumerated(EnumType.STRING)
    private SourceType sourceType; // UPLOAD, RTSP, EMAIL

    private Long durationSeconds;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum SourceType {
        UPLOAD, RTSP, EMAIL
    }
}