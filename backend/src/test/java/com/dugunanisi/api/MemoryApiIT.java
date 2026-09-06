package com.dugunanisi.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemoryApiIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createThenListNewestFirst() throws Exception {
		mockMvc.perform(post("/api/memories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  \",\"message\":\"  İlk not  \"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.name").value("Anonim"))
				.andExpect(jsonPath("$.message").value("İlk not"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		mockMvc.perform(post("/api/memories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  Ayşe  \",\"message\":\"İkinci not\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Ayşe"));

		mockMvc.perform(get("/api/memories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].message").value("İkinci not"))
				.andExpect(jsonPath("$[1].message").value("İlk not"));
	}

	@Test
	void rejectsMessageOverLimit() throws Exception {
		String tooLong = "a".repeat(601);

		mockMvc.perform(post("/api/memories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"" + tooLong + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Anı en fazla 600 karakter olabilir."));
	}
}
