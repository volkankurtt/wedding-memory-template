package com.dugunanisi.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.dugunanisi.PhotoTestConfiguration;
import com.dugunanisi.storage.InMemoryObjectStorage;
import com.dugunanisi.support.TestImages;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PhotoTestConfiguration.class)
@Transactional
class PhotoApiIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InMemoryObjectStorage storage;

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
	void sameUploadIdDoesNotDuplicateReadyPhoto() throws Exception {
		var first = mockMvc.perform(multipart("/api/photos")
						.file(new MockMultipartFile("file", "masa.jpg", "image/jpeg", TestImages.JPEG))
						.param("uploadId", "cccccccc-cccc-cccc-cccc-cccccccccccc"))
				.andExpect(status().isCreated())
				.andReturn();
		String body = first.getResponse().getContentAsString();
		String id = body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		mockMvc.perform(multipart("/api/photos")
						.file(new MockMultipartFile("file", "masa.jpg", "image/jpeg", TestImages.JPEG))
						.param("uploadId", "cccccccc-cccc-cccc-cccc-cccccccccccc"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(id));

		mockMvc.perform(get("/api/photos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photos", hasSize(1)));
	}

	@Test
	void rejectsGifWith415() throws Exception {
		mockMvc.perform(multipart("/api/photos").file(
						new MockMultipartFile("file", "x.gif", "image/gif", TestImages.GIF)))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error").value("Desteklenmeyen bir format. JPG, PNG, WEBP veya HEIC kullanın."));
	}

	@Test
	void signedUploadSessionThenFinalizeListsReadyPhoto() throws Exception {
		var session = mockMvc.perform(post("/api/photos/upload-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"dddddddd-dddd-dddd-dddd-dddddddddddd","fileName":"masa.jpg","contentType":"image/jpeg","sizeBytes":%d}
								""".formatted(TestImages.JPEG.length)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.needsUpload").value(true))
				.andExpect(jsonPath("$.path").value(org.hamcrest.Matchers.endsWith("/original")))
				.andReturn();
		String body = session.getResponse().getContentAsString();
		String photoId = body.replaceAll(".*\"photoId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		String path = body.replaceAll(".*\"path\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		storage.put(path, TestImages.JPEG, "image/jpeg");
		byte[] originalBefore = storage.get(path);

		mockMvc.perform(post("/api/photos/" + photoId + "/finalize")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"dddddddd-dddd-dddd-dddd-dddddddddddd"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(photoId))
				.andExpect(jsonPath("$.displayUrl").value(endsWith("/display.jpg")))
				.andExpect(jsonPath("$.originalUrl").value(endsWith("/original")));

		assertThat(storage.get(path)).isEqualTo(originalBefore);
		assertThat(storage.get(path)).isEqualTo(TestImages.JPEG);

		mockMvc.perform(post("/api/photos/" + photoId + "/finalize")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"dddddddd-dddd-dddd-dddd-dddddddddddd"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(photoId));

		mockMvc.perform(get("/api/photos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photos", hasSize(1)));
	}

	@Test
	void duplicateSessionDoesNotCreateSecondPhoto() throws Exception {
		String payload = """
				{"clientUploadId":"eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee","fileName":"masa.jpg","contentType":"image/jpeg","sizeBytes":%d}
				""".formatted(TestImages.JPEG.length);
		var first = mockMvc.perform(post("/api/photos/upload-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn();
		String id = first.getResponse().getContentAsString().replaceAll(".*\"photoId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		String path = id + "/original";
		storage.put(path, TestImages.JPEG, "image/jpeg");
		mockMvc.perform(post("/api/photos/" + id + "/finalize")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/photos/upload-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.photoId").value(id))
				.andExpect(jsonPath("$.alreadyReady").value(true))
				.andExpect(jsonPath("$.needsUpload").value(false));

		mockMvc.perform(get("/api/photos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photos", hasSize(1)));
	}

	@Test
	void finalizeWithoutObjectReturns400() throws Exception {
		var session = mockMvc.perform(post("/api/photos/upload-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"ffffffff-ffff-ffff-ffff-ffffffffffff","fileName":"masa.jpg","contentType":"image/jpeg","sizeBytes":%d}
								""".formatted(TestImages.JPEG.length)))
				.andExpect(status().isCreated())
				.andReturn();
		String photoId = session.getResponse().getContentAsString()
				.replaceAll(".*\"photoId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		mockMvc.perform(post("/api/photos/" + photoId + "/finalize")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"ffffffff-ffff-ffff-ffff-ffffffffffff"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uploadSessionRejectsGifAndOversize() throws Exception {
		mockMvc.perform(post("/api/photos/upload-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11","fileName":"x.gif","contentType":"image/gif","sizeBytes":12}
								"""))
				.andExpect(status().isUnsupportedMediaType());
		mockMvc.perform(post("/api/photos/upload-session")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"clientUploadId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12","fileName":"big.jpg","contentType":"image/jpeg","sizeBytes":26214401}
								"""))
				.andExpect(status().isPayloadTooLarge());
	}
}
