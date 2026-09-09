package com.dugunanisi.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dugunanisi.config.AppProperties;
import com.dugunanisi.domain.Photo;
import com.dugunanisi.domain.PhotoRepository;
import com.dugunanisi.service.PhotoService;
import com.dugunanisi.storage.InMemoryObjectStorage;

@ExtendWith(MockitoExtension.class)
class PhotoFinalizeOrientationTest {

	@Mock
	private PhotoRepository photos;

	@Test
	void finalizeCanvasJpegWithoutExifWritesDisplay() throws Exception {
		byte[] original = OrientedJpegs.solidHalves(64, 32);
		assertThat(JpegExifOrientation.read(original)).isEqualTo(1);
		assertOriginalUnchangedAfterFinalize(original);
	}

	@Test
	void finalizeBrokenIccJpegWritesDisplay() throws Exception {
		byte[] original = OrientedJpegs.withBrokenIccProfile(OrientedJpegs.solidHalves(64, 32));
		assertOriginalUnchangedAfterFinalize(original);
	}

	@Test
	void finalizeOrientation6KeepsOriginalBytes() throws Exception {
		assertOriginalUnchangedAfterFinalize(OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 6, true));
	}

	@Test
	void finalizeOrientation8KeepsOriginalBytes() throws Exception {
		assertOriginalUnchangedAfterFinalize(OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 8, false));
	}

	private void assertOriginalUnchangedAfterFinalize(byte[] original) throws Exception {
		InMemoryObjectStorage storage = new InMemoryObjectStorage();
		AppProperties properties = new AppProperties();
		properties.getSupabase().setUrl("https://example.supabase.co");
		properties.getSupabase().setStorageBucket("guest-photos");
		properties.getImageMagick().setCommand("magick-does-not-exist");
		PhotoService service = new PhotoService(photos, new ImageTypeDetector(),
				new ImageMagickImageConverter(properties), storage, properties);

		UUID id = UUID.randomUUID();
		String uploadId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
		Photo pending = new Photo(id, "iphone.jpg", "image/jpeg", original.length, id + "/original", uploadId);
		when(photos.findById(id)).thenReturn(Optional.of(pending));
		when(photos.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));
		storage.put(id + "/original", original.clone(), "image/jpeg");

		var response = service.finalizeUpload(id, uploadId);

		assertThat(pending.getStatus()).isEqualTo(com.dugunanisi.domain.PhotoStatus.READY);
		assertThat(storage.get(id + "/original")).isEqualTo(original);
		assertThat(response.displayUrl()).endsWith("/display.jpg");
		assertThat(response.originalUrl()).endsWith("/original");
		assertThat(JpegExifOrientation.read(storage.get(id + "/display.jpg"))).isEqualTo(1);
		assertThat(OrientedJpegs.read(storage.get(id + "/display.jpg"))).isNotNull();
	}
}
