package com.dugunanisi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.dugunanisi.api.ApiException;
import com.dugunanisi.config.AppProperties;
import com.dugunanisi.domain.Photo;
import com.dugunanisi.domain.PhotoRepository;
import com.dugunanisi.domain.PhotoStatus;
import com.dugunanisi.image.DetectedImageType;
import com.dugunanisi.image.ImageConversionException;
import com.dugunanisi.image.ImageConverter;
import com.dugunanisi.image.ImageTypeDetector;
import com.dugunanisi.storage.ObjectStorage;
import com.dugunanisi.support.TestImages;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

	@Mock
	private PhotoRepository photos;

	@Mock
	private ImageConverter converter;

	@Mock
	private ObjectStorage storage;

	private PhotoService service;

	@BeforeEach
	void setUp() {
		AppProperties properties = new AppProperties();
		properties.getSupabase().setUrl("https://example.supabase.co");
		properties.getSupabase().setStorageBucket("guest-photos");
		service = new PhotoService(photos, new ImageTypeDetector(), converter, storage, properties);
		lenient().when(photos.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void uploadsHeicWithout415AndStoresUuidPaths() {
		when(converter.toDisplayJpeg(any(), eq(DetectedImageType.HEIC))).thenReturn(TestImages.JPEG);
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"IMG_1000.HEIC",
				"image/heic",
				TestImages.heicHeader());

		var response = service.upload(file);

		assertThat(response.fileName()).isEqualTo("IMG_1000.HEIC");
		assertThat(response.fileUrl()).startsWith("https://example.supabase.co/storage/v1/object/public/guest-photos/");
		assertThat(response.fileUrl()).endsWith("/display.jpg");
		assertThat(response.displayUrl()).isEqualTo(response.fileUrl());
		assertThat(response.originalUrl()).endsWith("/original");
		assertThat(response.originalUrl()).doesNotContain("display.jpg");
		assertThat(response.fileUrl()).doesNotContain("IMG_1000");

		ArgumentCaptor<Photo> captor = ArgumentCaptor.forClass(Photo.class);
		verify(photos, times(2)).save(captor.capture());
		Photo last = captor.getValue();
		assertThat(last.getStatus()).isEqualTo(PhotoStatus.READY);
		assertThat(last.getStoragePath()).isEqualTo(last.getId() + "/original");
		verify(storage).put(eq(last.getId() + "/original"), any(), eq("image/heic"));
		verify(storage).put(eq(last.getId() + "/display.jpg"), eq(TestImages.JPEG), eq("image/jpeg"));
	}

	@Test
	void rejectsUnsupportedMagicBytesWith415() {
		MockMultipartFile file = new MockMultipartFile("file", "x.gif", "image/gif", TestImages.GIF);

		assertThatThrownBy(() -> service.upload(file))
				.isInstanceOf(ApiException.class)
				.hasMessage("Desteklenmeyen bir format. JPG, PNG, WEBP veya HEIC kullanın.")
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		verify(storage, never()).put(anyString(), any(), anyString());
	}

	@Test
	void rejectsOversizedFileWith413() {
		byte[] huge = new byte[(int) PhotoService.MAX_FILE_SIZE_BYTES + 1];
		huge[0] = (byte) 0xFF;
		huge[1] = (byte) 0xD8;
		huge[2] = (byte) 0xFF;
		MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", huge);

		assertThatThrownBy(() -> service.upload(file))
				.isInstanceOf(ApiException.class)
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
	}

	@Test
	void conversionFailureMarksFailed() {
		when(converter.toDisplayJpeg(any(), any())).thenThrow(new ImageConversionException("boom"));
		MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", TestImages.JPEG);

		assertThatThrownBy(() -> service.upload(file))
				.isInstanceOf(ApiException.class)
				.hasMessage("Fotoğraf görüntüye dönüştürülemedi. Lütfen tekrar deneyin.")
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

		ArgumentCaptor<Photo> captor = ArgumentCaptor.forClass(Photo.class);
		verify(photos, times(2)).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(PhotoStatus.FAILED);
	}

	@Test
	void listReadyUsesDefaultPageAndNewestFirst() {
		when(photos.findByStatus(eq(PhotoStatus.READY), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 30), 0));

		var response = service.listReady(0, 30);

		assertThat(response.page()).isEqualTo(0);
		assertThat(response.size()).isEqualTo(30);
		assertThat(response.photos()).isEmpty();
		assertThat(response.hasNext()).isFalse();

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(photos).findByStatus(eq(PhotoStatus.READY), captor.capture());
		Pageable pageable = captor.getValue();
		assertThat(pageable.getPageNumber()).isEqualTo(0);
		assertThat(pageable.getPageSize()).isEqualTo(30);
		assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	@Test
	void listReadyReportsHasNext() {
		Photo first = readyPhoto("a.jpg");
		when(photos.findByStatus(eq(PhotoStatus.READY), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 30), 61));

		var response = service.listReady(0, 30);

		assertThat(response.photos()).hasSize(1);
		assertThat(response.totalElements()).isEqualTo(61);
		assertThat(response.totalPages()).isEqualTo(3);
		assertThat(response.hasNext()).isTrue();
	}

	@Test
	void listReadyClampsSizeAboveMax() {
		when(photos.findByStatus(eq(PhotoStatus.READY), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, PhotoService.MAX_PAGE_SIZE), 0));

		service.listReady(0, 120);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(photos).findByStatus(eq(PhotoStatus.READY), captor.capture());
		assertThat(captor.getValue().getPageSize()).isEqualTo(PhotoService.MAX_PAGE_SIZE);
		assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
	}

	private static Photo readyPhoto(String fileName) {
		UUID id = UUID.randomUUID();
		Photo photo = new Photo(id, fileName, "image/jpeg", 10, id + "/original");
		photo.markReady("https://example.supabase.co/storage/v1/object/public/guest-photos/" + id + "/display.jpg");
		return photo;
	}
}
