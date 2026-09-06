package com.dugunanisi.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

	List<Memory> findAllByOrderByCreatedAtDesc();
}
