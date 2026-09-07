package com.dugunanisi.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dugunanisi.config.AppProperties;
import com.dugunanisi.support.TestImages;

class ImageMagickImageConverterTest {

	@Test
	void jpegIsConvertedWithImageIoWithoutMagick() {
		AppProperties properties = new AppProperties();
		properties.getImageMagick().setCommand("magick-does-not-exist");
		ImageMagickImageConverter converter = new ImageMagickImageConverter(properties);

		byte[] jpeg = converter.toDisplayJpeg(TestImages.JPEG, DetectedImageType.JPEG);

		assertThat(jpeg[0]).isEqualTo((byte) 0xFF);
		assertThat(jpeg[1]).isEqualTo((byte) 0xD8);
	}

	@Test
	void unreadableJpegFailsFastWithoutMagick() {
		AppProperties properties = new AppProperties();
		properties.getImageMagick().setCommand("magick-does-not-exist");
		ImageMagickImageConverter converter = new ImageMagickImageConverter(properties);
		byte[] brokenJpeg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2, 3, 4, 5, 6, 7, 8 };

		assertThatThrownBy(() -> converter.toDisplayJpeg(brokenJpeg, DetectedImageType.JPEG))
				.isInstanceOf(ImageConversionException.class)
				.hasMessageContaining("dönüştürülemedi");
	}

	@Test
	void heicWithoutMagickFailsFast() {
		AppProperties properties = new AppProperties();
		properties.getImageMagick().setCommand("magick-does-not-exist");
		ImageMagickImageConverter converter = new ImageMagickImageConverter(properties);

		assertThatThrownBy(() -> converter.toDisplayJpeg(TestImages.heicHeader(), DetectedImageType.HEIC))
				.isInstanceOf(ImageConversionException.class)
				.hasMessageContaining("ImageMagick");
	}
}
