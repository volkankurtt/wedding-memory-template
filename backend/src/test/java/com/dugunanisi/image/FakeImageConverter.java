package com.dugunanisi.image;

import com.dugunanisi.support.TestImages;

public class FakeImageConverter implements ImageConverter {

	@Override
	public byte[] toDisplayJpeg(byte[] original, DetectedImageType type) {
		return TestImages.JPEG;
	}
}
