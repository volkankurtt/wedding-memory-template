package com.dugunanisi.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dugunanisi.api.ApiException;
import com.dugunanisi.api.dto.CreateMemoryRequest;
import com.dugunanisi.api.dto.MemoryResponse;
import com.dugunanisi.domain.Memory;
import com.dugunanisi.domain.MemoryRepository;

@Service
public class MemoryService {

	public static final int MAX_MESSAGE_CHARS = 600;
	public static final int MAX_NAME_CHARS = 80;
	public static final String ANONYMOUS_NAME = "Anonim";

	private final MemoryRepository memories;

	public MemoryService(MemoryRepository memories) {
		this.memories = memories;
	}

	@Transactional(readOnly = true)
	public List<MemoryResponse> list() {
		return memories.findAllByOrderByCreatedAtDesc().stream().map(MemoryResponse::from).toList();
	}

	@Transactional
	public MemoryResponse create(CreateMemoryRequest request) {
		String message = request == null ? "" : trimToEmpty(request.message());
		if (message.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Lütfen bir anı veya mesaj yazın.");
		}
		if (message.length() > MAX_MESSAGE_CHARS) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Anı en fazla " + MAX_MESSAGE_CHARS + " karakter olabilir.");
		}

		String name = request == null ? "" : trimToEmpty(request.name());
		if (name.isEmpty()) {
			name = ANONYMOUS_NAME;
		}
		if (name.length() > MAX_NAME_CHARS) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ad en fazla " + MAX_NAME_CHARS + " karakter olabilir.");
		}

		Memory saved = memories.save(new Memory(name, message));
		return MemoryResponse.from(saved);
	}

	private static String trimToEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}
