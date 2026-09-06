package com.dugunanisi.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dugunanisi.support.TestImages;

class ImageTypeDetectorTest {

	private final ImageTypeDetector detector = new ImageTypeDetector();

	@Test
	void detectsJpegPngWebpAndHeic() {
		assertThat(detector.detect(TestImages.JPEG)).isEqualTo(DetectedImageType.JPEG);
		assertThat(detector.detect(pngHeader())).isEqualTo(DetectedImageType.PNG);
		assertThat(detector.detect(webpHeader())).isEqualTo(DetectedImageType.WEBP);
		assertThat(detector.detect(TestImages.heicHeader())).isEqualTo(DetectedImageType.HEIC);
	}

	@Test
	void rejectsGifAndShortPayloads() {
		assertThat(detector.detect(TestImages.GIF)).isNull();
		assertThat(detector.detect(new byte[] { 1, 2, 3 })).isNull();
	}

	@Test
	void allowsHeicMimeAndRejectsGifMime() {
		assertThat(detector.isAllowedMime("image/heic")).isTrue();
		assertThat(detector.isAllowedMime("image/heif")).isTrue();
		assertThat(detector.isAllowedMime("application/octet-stream")).isTrue();
		assertThat(detector.isAllowedMime("")).isTrue();
		assertThat(detector.isAllowedMime("image/gif")).isFalse();
	}

	private static byte[] pngHeader() {
		return new byte[] {
				(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
				0, 0, 0, 0, 0, 0, 0, 0
		};
	}

	private static byte[] webpHeader() {
		byte[] bytes = new byte[12];
		System.arraycopy("RIFF".getBytes(), 0, bytes, 0, 4);
		System.arraycopy("WEBP".getBytes(), 0, bytes, 8, 4);
		return bytes;
	}
}
