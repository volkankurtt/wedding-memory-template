package com.dugunanisi.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, String>> handleApi(ApiException exception) {
		return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "İstek gövdesi okunamadı."));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleTooLarge(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(Map.of("error", "Dosya 25 MB sınırını aşıyor."));
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<Map<String, String>> handleMultipart(MultipartException exception) {
		if (exception instanceof MaxUploadSizeExceededException
				|| exception.getCause() instanceof MaxUploadSizeExceededException) {
			return handleTooLarge(new MaxUploadSizeExceededException(25L * 1024 * 1024));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Yükleme isteği işlenemedi."));
	}
}
