package com.dugunanisi.api;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.dugunanisi.PhotoTestConfiguration;
import com.dugunanisi.support.TestImages;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PhotoTestConfiguration.class)
@Transactional
class PhotoApiIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void uploadJpegThenListReadyDisplayUrl() throws Exception {
		mockMvc.perform(multipart("/api/photos").file(
						new MockMultipartFile("file", "masa.jpg", "image/jpeg", TestImages.JPEG)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.fileName").value("masa.jpg"))
				.andExpect(jsonPath("$.fileUrl").value(endsWith("/display.jpg")))
				.andExpect(jsonPath("$.displayUrl").value(endsWith("/display.jpg")))
				.andExpect(jsonPath("$.originalUrl").value(endsWith("/original")))
				.andExpect(jsonPath("$.fileUrl").value(not(org.hamcrest.Matchers.containsString("masa.jpg"))))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		mockMvc.perform(get("/api/photos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photos", hasSize(1)))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(30))
				.andExpect(jsonPath("$.hasNext").value(false))
				.andExpect(jsonPath("$.photos[0].fileName").value("masa.jpg"))
				.andExpect(jsonPath("$.photos[0].fileUrl").value(endsWith("/display.jpg")))
				.andExpect(jsonPath("$.photos[0].displayUrl").value(endsWith("/display.jpg")))
				.andExpect(jsonPath("$.photos[0].originalUrl").value(endsWith("/original")));
	}

	@Test
	void rejectsGifWith415() throws Exception {
		mockMvc.perform(multipart("/api/photos").file(
						new MockMultipartFile("file", "x.gif", "image/gif", TestImages.GIF)))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error").value("Desteklenmeyen bir format. JPG, PNG, WEBP veya HEIC kullanın."));
	}
}
