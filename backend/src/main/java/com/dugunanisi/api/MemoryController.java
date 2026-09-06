package com.dugunanisi.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dugunanisi.api.dto.CreateMemoryRequest;
import com.dugunanisi.api.dto.MemoryResponse;
import com.dugunanisi.service.MemoryService;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

	private final MemoryService memories;

	public MemoryController(MemoryService memories) {
		this.memories = memories;
	}

	@GetMapping
	public List<MemoryResponse> list() {
		return memories.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MemoryResponse create(@RequestBody(required = false) CreateMemoryRequest request) {
		return memories.create(request);
	}
}
