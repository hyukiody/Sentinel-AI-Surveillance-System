package com.enterprise.sentinel.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VideoEntityTest {

    @Test
    @DisplayName("PrePersist should automatically set createdAt")
    void prePersistLifecycle() {
        Video video = Video.builder()
                .originalFilename("test.mp4")
                .build();

        // Simulate JPA PrePersist manually since we are in a unit test
        // In a DataJpaTest, the EntityListener would trigger this.
        // Here we just verify the method logic itself.
        video.onCreate();

        assertThat(video.getCreatedAt()).isNotNull();
        assertThat(video.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Builder should construct valid object")
    void builderConstruction() {
        Video video = Video.builder()
                .originalFilename("cam_01.mp4")
                .sourceType(Video.SourceType.RTSP)
                .durationSeconds(120L)
                .build();

        assertThat(video.getOriginalFilename()).isEqualTo("cam_01.mp4");
        assertThat(video.getSourceType()).isEqualTo(Video.SourceType.RTSP);
        assertThat(video.getDurationSeconds()).isEqualTo(120L);
    }
}