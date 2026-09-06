package com.dugunanisi.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.dugunanisi.domain.Photo;

public record PhotoResponse(UUID id, String fileName, String fileUrl, Instant createdAt) {

	public static PhotoResponse from(Photo photo) {
		return new PhotoResponse(photo.getId(), photo.getOriginalFileName(), photo.getPublicPath(), photo.getCreatedAt());
	}
}
