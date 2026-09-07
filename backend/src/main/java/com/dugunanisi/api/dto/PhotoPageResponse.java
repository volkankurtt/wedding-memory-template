package com.dugunanisi.api.dto;

import java.util.List;

public record PhotoPageResponse(
		List<PhotoResponse> photos,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext) {
}
