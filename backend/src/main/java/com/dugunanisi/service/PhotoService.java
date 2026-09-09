package com.dugunanisi.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dugunanisi.api.ApiException;
import com.dugunanisi.api.dto.CreateUploadSessionRequest;
import com.dugunanisi.api.dto.PhotoPageResponse;
import com.dugunanisi.api.dto.PhotoResponse;
import com.dugunanisi.api.dto.UploadSessionResponse;
import com.dugunanisi.config.AppProperties;
import com.dugunanisi.domain.Photo;
import com.dugunanisi.domain.PhotoRepository;
import com.dugunanisi.domain.PhotoStatus;
import com.dugunanisi.image.DetectedImageType;
import com.dugunanisi.image.ImageConversionException;
import com.dugunanisi.image.ImageConverter;
import com.dugunanisi.image.ImageTypeDetector;
import com.dugunanisi.storage.ObjectStorage;
import com.dugunanisi.storage.SignedUpload;
import com.dugunanisi.storage.StorageException;

@Service
public class PhotoService {

	public static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;
	public static final int DEFAULT_PAGE_SIZE = 30;
	public static final int MAX_PAGE_SIZE = 60;
	/** Supabase signed upload tokens last 2 hours; expiry is not configurable. */
	public static final int SIGNED_UPLOAD_TTL_SECONDS = 2 * 60 * 60;
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
		return upload(file, null);
	}

	@Transactional
	public PhotoResponse upload(MultipartFile file, String clientUploadId) {
		long started = System.nanoTime();
		String uploadKey = parseUploadId(clientUploadId);
		log.info("POST /api/photos received empty={} size={} contentType={} name={} uploadId={}",
				file == null || file.isEmpty(),
				file == null ? -1 : file.getSize(),
				file == null ? null : file.getContentType(),
				file == null ? null : safeFileName(file.getOriginalFilename()),
				uploadKey);
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

		if (uploadKey != null) {
			Optional<Photo> existing = photos.findByClientUploadId(uploadKey);
			if (existing.isPresent() && existing.get().getStatus() == PhotoStatus.READY) {
				log.info("Idempotent upload replay id={} uploadId={}", existing.get().getId(), uploadKey);
				return PhotoResponse.from(existing.get());
			}
			if (existing.isPresent()) {
				return storeAndComplete(existing.get(), bytes, type, started);
			}
		}

		UUID id = UUID.randomUUID();
		String originalPath = id + "/original";
		String fileName = safeFileName(file.getOriginalFilename());
		log.info("Photo upload started id={} type={} size={} name={} uploadId={}", id, type, bytes.length, fileName, uploadKey);
		Photo photo;
		try {
			photo = photos.save(new Photo(id, fileName, type.contentType(), bytes.length, originalPath, uploadKey));
		}
		catch (DataIntegrityViolationException exception) {
			if (uploadKey == null) {
				throw exception;
			}
			Photo raced = photos.findByClientUploadId(uploadKey)
					.orElseThrow(() -> exception);
			if (raced.getStatus() == PhotoStatus.READY) {
				log.info("Idempotent upload replay after race id={} uploadId={}", raced.getId(), uploadKey);
				return PhotoResponse.from(raced);
			}
			return storeAndComplete(raced, bytes, type, started);
		}
		return storeAndComplete(photo, bytes, type, started, true);
	}

	@Transactional
	public UploadSessionResponse createUploadSession(CreateUploadSessionRequest request) {
		if (request == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Yükleme isteği işlenemedi.");
		}
		String uploadKey = parseUploadId(request.clientUploadId());
		if (uploadKey == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Yükleme isteği işlenemedi.");
		}
		if (request.sizeBytes() <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Lütfen bir fotoğraf seçin.");
		}
		if (request.sizeBytes() > MAX_FILE_SIZE_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Dosya 25 MB sınırını aşıyor.");
		}
		if (!detector.isAllowedMime(request.contentType())) {
			throw unsupportedType();
		}

		Optional<Photo> existing = photos.findByClientUploadId(uploadKey);
		if (existing.isPresent()) {
			return sessionForExisting(existing.get(), uploadKey);
		}

		UUID id = UUID.randomUUID();
		String originalPath = id + "/original";
		String fileName = safeFileName(request.fileName());
		String contentType = request.contentType() == null || request.contentType().isBlank()
				? "application/octet-stream"
				: request.contentType();
		Photo photo;
		try {
			photo = photos.save(new Photo(id, fileName, contentType, request.sizeBytes(), originalPath, uploadKey));
		}
		catch (DataIntegrityViolationException exception) {
			Photo raced = photos.findByClientUploadId(uploadKey).orElseThrow(() -> exception);
			return sessionForExisting(raced, uploadKey);
		}
		return signedSession(photo, uploadKey);
	}

	@Transactional
	public PhotoResponse finalizeUpload(UUID photoId, String clientUploadId) {
		long started = System.nanoTime();
		String uploadKey = parseUploadId(clientUploadId);
		if (uploadKey == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Yükleme isteği işlenemedi.");
		}
		Photo photo = photos.findById(photoId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fotoğraf bulunamadı."));
		if (photo.getClientUploadId() == null || !uploadKey.equals(photo.getClientUploadId())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Yükleme isteği işlenemedi.");
		}
		if (photo.getStatus() == PhotoStatus.READY) {
			log.info("Idempotent finalize replay id={} uploadId={}", photo.getId(), uploadKey);
			return PhotoResponse.from(photo);
		}

		String originalPath = photo.getStoragePath();
		if (!storage.exists(originalPath)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Fotoğraf henüz yüklenmedi. Lütfen tekrar deneyin.");
		}
		byte[] bytes;
		try {
			bytes = storage.get(originalPath);
		}
		catch (StorageException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Fotoğraf henüz yüklenmedi. Lütfen tekrar deneyin.");
		}
		if (bytes.length > MAX_FILE_SIZE_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Dosya 25 MB sınırını aşıyor.");
		}
		DetectedImageType type = detector.detect(bytes);
		if (type == null) {
			photo.markFailed();
			photos.save(photo);
			throw unsupportedType();
		}
		return storeAndComplete(photo, bytes, type, started, false);
	}

	private UploadSessionResponse sessionForExisting(Photo photo, String uploadKey) {
		if (photo.getStatus() == PhotoStatus.READY) {
			log.info("Idempotent session replay id={} uploadId={}", photo.getId(), uploadKey);
			return new UploadSessionResponse(
					photo.getId(),
					uploadKey,
					photo.getStoragePath(),
					null,
					null,
					SIGNED_UPLOAD_TTL_SECONDS,
					true,
					false,
					PhotoResponse.from(photo));
		}
		boolean needsUpload = !storage.exists(photo.getStoragePath());
		if (!needsUpload) {
			return new UploadSessionResponse(
					photo.getId(),
					uploadKey,
					photo.getStoragePath(),
					null,
					null,
					SIGNED_UPLOAD_TTL_SECONDS,
					false,
					false,
					null);
		}
		return signedSession(photo, uploadKey);
	}

	private UploadSessionResponse signedSession(Photo photo, String uploadKey) {
		try {
			SignedUpload signed = storage.createSignedUpload(
					photo.getStoragePath(),
					photo.getContentType(),
					Duration.ofSeconds(SIGNED_UPLOAD_TTL_SECONDS));
			return new UploadSessionResponse(
					photo.getId(),
					uploadKey,
					photo.getStoragePath(),
					signed.signedUrl(),
					signed.token(),
					signed.expiresInSeconds(),
					false,
					true,
					null);
		}
		catch (StorageException exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Yükleme adresi üretilemedi. Lütfen tekrar deneyin.");
		}
	}

	private PhotoResponse storeAndComplete(Photo photo, byte[] bytes, DetectedImageType type, long started) {
		return storeAndComplete(photo, bytes, type, started, true);
	}

	private PhotoResponse storeAndComplete(
			Photo photo,
			byte[] bytes,
			DetectedImageType type,
			long started,
			boolean storeOriginal) {
		UUID id = photo.getId();
		String originalPath = photo.getStoragePath();
		String displayPath = id + "/display.jpg";
		try {
			if (storeOriginal) {
				log.info("Uploading original to storage path={}", originalPath);
				long originalStarted = System.nanoTime();
				storage.put(originalPath, bytes, type.contentType());
				log.info("Original stored id={} ms={} sinceStartMs={}", id, elapsedMs(originalStarted), elapsedMs(started));
			}
			else {
				log.info("Finalize using stored original path={} bytes={}", originalPath, bytes.length);
			}
			long convertStarted = System.nanoTime();
			byte[] displayJpeg = converter.toDisplayJpeg(bytes, type, photo.getOriginalFileName());
			log.info("Display generated id={} file={} contentType={} ms={} sinceStartMs={} bytes={} path={}",
					id,
					photo.getOriginalFileName(),
					type.contentType(),
					elapsedMs(convertStarted),
					elapsedMs(started),
					displayJpeg.length,
					displayPath);
			long displayStoreStarted = System.nanoTime();
			storage.put(displayPath, displayJpeg, "image/jpeg");
			log.info("Display stored id={} ms={} sinceStartMs={} path={} totalMs={}",
					id, elapsedMs(displayStoreStarted), elapsedMs(started), displayPath, elapsedMs(started));
			photo.markReady(properties.getSupabase().publicObjectUrl(displayPath));
			PhotoResponse response = PhotoResponse.from(photos.save(photo));
			log.info("Upload response sent id={} file={} contentType={} displayBytes={} path={} totalMs={}",
					id,
					photo.getOriginalFileName(),
					type.contentType(),
					displayJpeg.length,
					displayPath,
					elapsedMs(started));
			return response;
		}
		catch (ImageConversionException | StorageException exception) {
			log.warn("Photo upload failed id={} status will be FAILED sinceStartMs={}: {}",
					id, elapsedMs(started), exception.toString(), exception);
			photo.markFailed();
			photos.save(photo);
			if (exception instanceof ImageConversionException) {
				throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Fotoğraf görüntüye dönüştürülemedi. Lütfen tekrar deneyin.");
			}
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Fotoğraf depolanamadı. Lütfen tekrar deneyin.");
		}
	}

	private static String parseUploadId(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(raw.trim()).toString();
		}
		catch (IllegalArgumentException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Yükleme isteği işlenemedi.");
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
