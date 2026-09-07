package com.dugunanisi.image;

public interface ImageConverter {

	byte[] toDisplayJpeg(byte[] original, DetectedImageType type);

	default byte[] toDisplayJpeg(byte[] original, DetectedImageType type, String originalFileName) {
		return toDisplayJpeg(original, type);
	}
}
