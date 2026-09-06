package com.dugunanisi.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dugunanisi.api.dto.PhotoResponse;
import com.dugunanisi.service.PhotoService;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

	private final PhotoService photos;

	public PhotoController(PhotoService photos) {
		this.photos = photos;
	}

	@GetMapping
	public List<PhotoResponse> list() {
		return photos.listReady();
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PhotoResponse upload(@RequestParam(value = "file", required = false) MultipartFile file) {
		return photos.upload(file);
	}
}
