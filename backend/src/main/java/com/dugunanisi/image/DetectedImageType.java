package com.dugunanisi.image;

public enum DetectedImageType {
	JPEG("image/jpeg", ".jpg"),
	PNG("image/png", ".png"),
	WEBP("image/webp", ".webp"),
	HEIC("image/heic", ".heic");

	private final String contentType;
	private final String fileSuffix;

	DetectedImageType(String contentType, String fileSuffix) {
		this.contentType = contentType;
		this.fileSuffix = fileSuffix;
	}

	public String contentType() {
		return contentType;
	}

	public String fileSuffix() {
		return fileSuffix;
	}
}
