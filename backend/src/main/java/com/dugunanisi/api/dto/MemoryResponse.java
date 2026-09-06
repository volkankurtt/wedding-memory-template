package com.dugunanisi.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.dugunanisi.domain.Memory;

public record MemoryResponse(UUID id, String name, String message, Instant createdAt) {

	public static MemoryResponse from(Memory memory) {
		return new MemoryResponse(memory.getId(), memory.getName(), memory.getMessage(), memory.getCreatedAt());
	}
}
