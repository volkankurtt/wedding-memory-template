package com.dugunanisi.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dugunanisi.api.dto.MemoryResponse;
import com.dugunanisi.service.MemoryService;

@WebMvcTest(controllers = MemoryController.class)
@Import(ApiExceptionHandler.class)
class MemoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemoryService memoryService;

	@Test
	void listReturnsJsonContract() throws Exception {
		UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
		when(memoryService.list()).thenReturn(List.of(
				new MemoryResponse(id, "Anonim", "Mutluluklar", Instant.parse("2026-09-07T12:00:00Z"))));

		mockMvc.perform(get("/api/memories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(id.toString()))
				.andExpect(jsonPath("$[0].name").value("Anonim"))
				.andExpect(jsonPath("$[0].message").value("Mutluluklar"))
				.andExpect(jsonPath("$[0].createdAt").value("2026-09-07T12:00:00Z"));
	}

	@Test
	void createReturnsTurkishErrorBody() throws Exception {
		when(memoryService.create(any())).thenThrow(
				new ApiException(HttpStatus.BAD_REQUEST, "Lütfen bir anı veya mesaj yazın."));

		mockMvc.perform(post("/api/memories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Ayşe\",\"message\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Lütfen bir anı veya mesaj yazın."));
	}
}
