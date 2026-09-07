package com.dugunanisi.api.dto;

public record CreateUploadSessionRequest(
		String clientUploadId,
		String fileName,
		String contentType,
		long sizeBytes) {
}
