package com.dugunanisi.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

	Page<Photo> findByStatus(PhotoStatus status, Pageable pageable);

	Optional<Photo> findByClientUploadId(String clientUploadId);
}
