package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {
    // Standard CRUD
}