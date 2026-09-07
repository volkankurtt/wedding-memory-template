package com.dugunanisi.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dugunanisi.api.ApiException;
import com.dugunanisi.api.dto.PhotoPageResponse;
import com.dugunanisi.api.dto.PhotoResponse;
import com.dugunanisi.config.AppProperties;
import com.dugunanisi.domain.Photo;
import com.dugunanisi.domain.PhotoRepository;
import com.dugunanisi.domain.PhotoStatus;
import com.dugunanisi.image.DetectedImageType;
import com.dugunanisi.image.ImageConversionException;
import com.dugunanisi.image.ImageConverter;
import com.dugunanisi.image.ImageTypeDetector;
import com.dugunanisi.storage.ObjectStorage;
import com.dugunanisi.storage.StorageException;

@Service
public class PhotoService {

	public static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;
	public static final int DEFAULT_PAGE_SIZE = 30;
	public static final int MAX_PAGE_SIZE = 60;
	private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

	private final PhotoRepository photos;
	private final ImageTypeDetector detector;
	private final ImageConverter converter;
	private final ObjectStorage storage;
	private final AppProperties properties;

	public PhotoService(
			PhotoRepository photos,
			ImageTypeDetector detector,
			ImageConverter converter,
			ObjectStorage storage,
			AppProperties properties) {
		this.photos = photos;
		this.detector = detector;
		this.converter = converter;
		this.storage = storage;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public PhotoPageResponse listReady(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Photo> result = photos.findByStatus(PhotoStatus.READY, pageable);
		return new PhotoPageResponse(
				result.getContent().stream().map(PhotoResponse::from).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.hasNext());
	}

	@Transactional
	public PhotoResponse upload(MultipartFile file) {
		long started = System.nanoTime();
		log.info("POST /api/photos received empty={} size={} contentType={} name={}",
				file == null || file.isEmpty(),
				file == null ? -1 : file.getSize(),
				file == null ? null : file.getContentType(),
				file == null ? null : safeFileName(file.getOriginalFilename()));
		if (file == null || file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Lütfen bir fotoğraf seçin.");
		}
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Dosya 25 MB sınırını aşıyor.");
		}
		if (!detector.isAllowedMime(file.getContentType())) {
			throw unsupportedType();
		}

		byte[] bytes = readBytes(file);
		if (bytes.length > MAX_FILE_SIZE_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Dosya 25 MB sınırını aşıyor.");
		}

		DetectedImageType type = detector.detect(bytes);
		if (type == null) {
			throw unsupportedType();
		}

		UUID id = UUID.randomUUID();
		String originalPath = id + "/original";
		String displayPath = id + "/display.jpg";
		log.info("Photo upload started id={} type={} size={} name={}", id, type, bytes.length, safeFileName(file.getOriginalFilename()));
		Photo photo = photos.save(new Photo(id, safeFileName(file.getOriginalFilename()), type.contentType(), bytes.length, originalPath));

		try {
			log.info("Uploading original to storage path={}", originalPath);
			long originalStarted = System.nanoTime();
			storage.put(originalPath, bytes, type.contentType());
			log.info("Original stored id={} ms={} sinceStartMs={}", id, elapsedMs(originalStarted), elapsedMs(started));
			long convertStarted = System.nanoTime();
			byte[] displayJpeg = converter.toDisplayJpeg(bytes, type);
			log.info("Display generated id={} ms={} sinceStartMs={} bytes={}",
					id, elapsedMs(convertStarted), elapsedMs(started), displayJpeg.length);
			long displayStoreStarted = System.nanoTime();
			storage.put(displayPath, displayJpeg, "image/jpeg");
			log.info("Display stored id={} ms={} sinceStartMs={} path={}",
					id, elapsedMs(displayStoreStarted), elapsedMs(started), displayPath);
			photo.markReady(properties.getSupabase().publicObjectUrl(displayPath));
			PhotoResponse response = PhotoResponse.from(photos.save(photo));
			log.info("Upload response sent id={} totalMs={}", id, elapsedMs(started));
			return response;
		}
		catch (ImageConversionException | StorageException exception) {
			log.warn("Photo upload failed id={} status will be FAILED sinceStartMs={}: {}",
					id, elapsedMs(started), exception.getMessage());
			photo.markFailed();
			photos.save(photo);
			if (exception instanceof ImageConversionException) {
				throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Fotoğraf görüntüye dönüştürülemedi. Lütfen tekrar deneyin.");
			}
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Fotoğraf depolanamadı. Lütfen tekrar deneyin.");
		}
	}

	private static ApiException unsupportedType() {
		return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"Desteklenmeyen bir format. JPG, PNG, WEBP veya HEIC kullanın.");
	}

	private static byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Dosya okunamadı.");
		}
	}

	private static String safeFileName(String original) {
		if (original == null || original.isBlank()) {
			return "photo.jpg";
		}
		String name = Path.of(original.replace('\\', '/')).getFileName().toString().trim();
		return name.isEmpty() ? "photo.jpg" : name;
	}

	private static long elapsedMs(long startedNanos) {
		return (System.nanoTime() - startedNanos) / 1_000_000L;
	}
}
