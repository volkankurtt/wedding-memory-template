package com.dugunanisi.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dugunanisi.domain.Photo;

class PhotoResponseTest {

	@Test
	void mapsExistingPhotoDisplayAndOriginalUrls() {
		UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
		Photo photo = new Photo(id, "IMG_9000.HEIC", "image/heic", 2048, id + "/original");
		photo.markReady("https://example.supabase.co/storage/v1/object/public/guest-photos/" + id + "/display.jpg");

		PhotoResponse response = PhotoResponse.from(photo);

		assertThat(response.fileName()).isEqualTo("IMG_9000.HEIC");
		assertThat(response.fileUrl()).endsWith("/display.jpg");
		assertThat(response.displayUrl()).isEqualTo(response.fileUrl());
		assertThat(response.originalUrl()).isEqualTo(
				"https://example.supabase.co/storage/v1/object/public/guest-photos/" + id + "/original");
		assertThat(response.originalUrl()).doesNotContain("display.jpg");
	}
}
