package com.dugunanisi.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dugunanisi.api.ApiException;
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
	public List<PhotoResponse> listReady() {
		return photos.findByStatusOrderByCreatedAtDesc(PhotoStatus.READY).stream().map(PhotoResponse::from).toList();
	}

	@Transactional
	public PhotoResponse upload(MultipartFile file) {
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
			storage.put(originalPath, bytes, type.contentType());
			log.info("Original stored, converting to display.jpg id={}", id);
			byte[] displayJpeg = converter.toDisplayJpeg(bytes, type);
			log.info("Display jpeg ready id={} bytes={}", id, displayJpeg.length);
			storage.put(displayPath, displayJpeg, "image/jpeg");
			log.info("Display stored path={}", displayPath);
			photo.markReady(properties.getSupabase().publicObjectUrl(displayPath));
			return PhotoResponse.from(photos.save(photo));
		}
		catch (ImageConversionException | StorageException exception) {
			log.warn("Photo upload failed id={} status will be FAILED: {}", id, exception.getMessage());
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
}
