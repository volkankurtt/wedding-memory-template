package com.dugunanisi.image;

public interface ImageConverter {

	byte[] toDisplayJpeg(byte[] original, DetectedImageType type);
}
