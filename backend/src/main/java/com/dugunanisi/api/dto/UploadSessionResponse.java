package com.dugunanisi.api.dto;

import java.util.UUID;

public record UploadSessionResponse(
		UUID photoId,
		String clientUploadId,
		String path,
		String signedUrl,
		String token,
		int expiresInSeconds,
		boolean alreadyReady,
		boolean needsUpload,
		PhotoResponse photo) {
}
