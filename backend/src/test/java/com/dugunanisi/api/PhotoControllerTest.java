package com.dugunanisi.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dugunanisi.api.dto.PhotoResponse;
import com.dugunanisi.service.PhotoService;
import com.dugunanisi.support.TestImages;

@WebMvcTest(controllers = PhotoController.class)
@Import(ApiExceptionHandler.class)
class PhotoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PhotoService photoService;

	@Test
	void listReturnsFrontendContract() throws Exception {
		UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
		String displayUrl = "https://example.supabase.co/storage/v1/object/public/guest-photos/" + id + "/display.jpg";
		String originalUrl = "https://example.supabase.co/storage/v1/object/public/guest-photos/" + id + "/original";
		when(photoService.listReady()).thenReturn(List.of(new PhotoResponse(
				id,
				"masa.jpg",
				displayUrl,
				displayUrl,
				originalUrl,
				Instant.parse("2026-09-07T12:00:00Z"))));

		mockMvc.perform(get("/api/photos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(id.toString()))
				.andExpect(jsonPath("$[0].fileName").value("masa.jpg"))
				.andExpect(jsonPath("$[0].fileUrl").value(displayUrl))
				.andExpect(jsonPath("$[0].displayUrl").value(displayUrl))
				.andExpect(jsonPath("$[0].originalUrl").value(originalUrl))
				.andExpect(jsonPath("$[0].createdAt").value("2026-09-07T12:00:00Z"));
	}

	@Test
	void uploadMapsUnsupportedTypeTo415() throws Exception {
		when(photoService.upload(org.mockito.ArgumentMatchers.any())).thenThrow(
				new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
						"Desteklenmeyen bir format. JPG, PNG, WEBP veya HEIC kullanın."));

		mockMvc.perform(multipart("/api/photos").file(
						new MockMultipartFile("file", "x.gif", "image/gif", TestImages.GIF)))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error").value("Desteklenmeyen bir format. JPG, PNG, WEBP veya HEIC kullanın."));
	}
}
