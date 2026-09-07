package com.dugunanisi.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.dugunanisi.domain.Photo;

public record PhotoResponse(
		UUID id,
		String fileName,
		String fileUrl,
		String displayUrl,
		String originalUrl,
		Instant createdAt) {

	public static PhotoResponse from(Photo photo) {
		String displayUrl = photo.getPublicPath();
		return new PhotoResponse(
				photo.getId(),
				photo.getOriginalFileName(),
				displayUrl,
				displayUrl,
				originalUrl(photo),
				photo.getCreatedAt());
	}

	static String originalUrl(Photo photo) {
		String displayUrl = photo.getPublicPath();
		if (displayUrl == null || displayUrl.isBlank()) {
			return displayUrl;
		}
		int slash = displayUrl.lastIndexOf('/');
		if (slash < 0) {
			return displayUrl;
		}
		return displayUrl.substring(0, slash + 1) + objectName(photo.getStoragePath());
	}

	private static String objectName(String storagePath) {
		if (storagePath == null || storagePath.isBlank()) {
			return "original";
		}
		int slash = storagePath.lastIndexOf('/');
		return slash >= 0 ? storagePath.substring(slash + 1) : storagePath;
	}
}
