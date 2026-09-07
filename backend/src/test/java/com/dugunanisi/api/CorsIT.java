package com.dugunanisi.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsConfiguredDevOriginForPhotoPostPreflight() throws Exception {
		mockMvc.perform(options("/api/photos")
						.header("Origin", "http://localhost:5173")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
				.andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
	}

	@Test
	void allowsConfiguredDevOriginForUploadSessionPreflight() throws Exception {
		mockMvc.perform(options("/api/photos/upload-session")
						.header("Origin", "http://localhost:5173")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}

	@Test
	void rejectsUnknownOrigin() throws Exception {
		mockMvc.perform(options("/api/photos")
						.header("Origin", "https://evil.example")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void healthAllowsGetFromDevOrigin() throws Exception {
		mockMvc.perform(options("/api/health")
						.header("Origin", "http://localhost:5173")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}
}
