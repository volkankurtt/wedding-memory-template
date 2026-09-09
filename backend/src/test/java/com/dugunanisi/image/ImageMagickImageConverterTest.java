package com.dugunanisi.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.dugunanisi.config.AppProperties;
import com.dugunanisi.support.TestImages;

class ImageMagickImageConverterTest {

	@Test
	void jpegIsConvertedWithImageIoWithoutMagick() {
		byte[] jpeg = converter().toDisplayJpeg(TestImages.JPEG, DetectedImageType.JPEG);

		assertThat(jpeg[0]).isEqualTo((byte) 0xFF);
		assertThat(jpeg[1]).isEqualTo((byte) 0xD8);
	}

	@Test
	void orientation6JpegBecomesPortraitInCorrectDirection() throws Exception {
		byte[] original = OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 6, true);
		BufferedImage stored = OrientedJpegs.read(original);
		assertThat(stored.getWidth()).isEqualTo(64);
		assertThat(stored.getHeight()).isEqualTo(32);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(32);
		assertThat(image.getHeight()).isEqualTo(64);
		assertThat(OrientedJpegs.reddish(sample(image, 0.5, 0.25))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.5, 0.75))).isTrue();
		assertThat(JpegExifOrientation.read(display)).isEqualTo(1);
	}

	@Test
	void orientation8JpegRotatesCounterClockwise() throws Exception {
		byte[] original = OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 8, false);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(32);
		assertThat(image.getHeight()).isEqualTo(64);
		assertThat(OrientedJpegs.bluish(sample(image, 0.5, 0.25))).isTrue();
		assertThat(OrientedJpegs.reddish(sample(image, 0.5, 0.75))).isTrue();
	}

	@Test
	void orientation2JpegIsFlippedHorizontally() throws Exception {
		byte[] original = OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 2, true);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(64);
		assertThat(image.getHeight()).isEqualTo(32);
		assertThat(OrientedJpegs.bluish(sample(image, 0.25, 0.5))).isTrue();
		assertThat(OrientedJpegs.reddish(sample(image, 0.75, 0.5))).isTrue();
	}

	@Test
	void canvasReencodedJpegWithoutExifProducesDisplay() throws Exception {
		byte[] original = OrientedJpegs.solidHalves(64, 32);
		assertThat(JpegExifOrientation.read(original)).isEqualTo(1);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(64);
		assertThat(image.getHeight()).isEqualTo(32);
		assertThat(OrientedJpegs.reddish(sample(image, 0.25, 0.5))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.75, 0.5))).isTrue();
		assertThat(JpegExifOrientation.read(display)).isEqualTo(1);
	}

	@Test
	void jpegWithBrokenIccProfileStillProducesDisplay() throws Exception {
		byte[] original = OrientedJpegs.withBrokenIccProfile(OrientedJpegs.solidHalves(64, 32));
		assertThat(new ImageTypeDetector().detect(original)).isEqualTo(DetectedImageType.JPEG);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(64);
		assertThat(image.getHeight()).isEqualTo(32);
		assertThat(OrientedJpegs.reddish(sample(image, 0.25, 0.5))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.75, 0.5))).isTrue();
	}

	@Test
	void normalJpegKeepsPixelOrientation() throws Exception {
		byte[] original = OrientedJpegs.solidHalves(64, 32);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(64);
		assertThat(image.getHeight()).isEqualTo(32);
		assertThat(OrientedJpegs.reddish(sample(image, 0.25, 0.5))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.75, 0.5))).isTrue();
	}

	@Test
	void orientation3JpegRotates180() throws Exception {
		byte[] original = OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 3, true);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(64);
		assertThat(image.getHeight()).isEqualTo(32);
		assertThat(OrientedJpegs.bluish(sample(image, 0.25, 0.5))).isTrue();
		assertThat(OrientedJpegs.reddish(sample(image, 0.75, 0.5))).isTrue();
	}

	@Test
	void orientation4JpegIsFlippedVertically() throws Exception {
		byte[] original = OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 4, true);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(64);
		assertThat(image.getHeight()).isEqualTo(32);
		assertThat(OrientedJpegs.reddish(sample(image, 0.25, 0.5))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.75, 0.5))).isTrue();
	}

	@Test
	void orientation5And7SwapToPortrait() throws Exception {
		BufferedImage five = OrientedJpegs.read(converter().toDisplayJpeg(
				OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 5, true),
				DetectedImageType.JPEG));
		BufferedImage seven = OrientedJpegs.read(converter().toDisplayJpeg(
				OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(64, 32), 7, false),
				DetectedImageType.JPEG));

		assertThat(five.getWidth()).isEqualTo(32);
		assertThat(five.getHeight()).isEqualTo(64);
		assertThat(seven.getWidth()).isEqualTo(32);
		assertThat(seven.getHeight()).isEqualTo(64);
	}

	@Test
	void iphoneStyleOrientation6BecomesPortrait() throws Exception {
		byte[] original = OrientedJpegs.withIphoneStyleOrientation(OrientedJpegs.solidHalves(64, 32), 6);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(32);
		assertThat(image.getHeight()).isEqualTo(64);
		assertThat(OrientedJpegs.reddish(sample(image, 0.5, 0.25))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.5, 0.75))).isTrue();
	}

	@Test
	void originalJpegBytesAreNotMutated() throws Exception {
		byte[] original = OrientedJpegs.withIphoneStyleOrientation(OrientedJpegs.solidHalves(64, 32), 8);
		byte[] copy = original.clone();

		converter().toDisplayJpeg(original, DetectedImageType.JPEG);

		assertThat(original).isEqualTo(copy);
	}

	@Test
	void orientationIsAppliedBeforeLongEdgeResize() throws Exception {
		byte[] original = OrientedJpegs.withOrientation(OrientedJpegs.solidHalves(2000, 1000), 6, true);

		byte[] display = converter().toDisplayJpeg(original, DetectedImageType.JPEG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(960);
		assertThat(image.getHeight()).isEqualTo(1920);
		assertThat(OrientedJpegs.reddish(sample(image, 0.5, 0.25))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.5, 0.75))).isTrue();
	}

	@Test
	void pngIsNotRotatedByJpegExifRules() throws Exception {
		byte[] display = converter().toDisplayJpeg(pngHalves(40, 20), DetectedImageType.PNG);
		BufferedImage image = OrientedJpegs.read(display);

		assertThat(image.getWidth()).isEqualTo(40);
		assertThat(image.getHeight()).isEqualTo(20);
		assertThat(OrientedJpegs.reddish(sample(image, 0.25, 0.5))).isTrue();
		assertThat(OrientedJpegs.bluish(sample(image, 0.75, 0.5))).isTrue();
	}

	@Test
	void concurrentJpegFinalizeDoesNotFail() throws Exception {
		byte[] original = OrientedJpegs.solidHalves(96, 64);
		var converter = converter();
		var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
		var threads = new Thread[8];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				try {
					byte[] display = converter.toDisplayJpeg(original, DetectedImageType.JPEG);
					if (display.length < 3 || display[0] != (byte) 0xFF) {
						errors.add(new AssertionError("invalid jpeg"));
					}
				}
				catch (Throwable error) {
					errors.add(error);
				}
			});
		}
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		assertThat(errors).isEmpty();
	}

	@Test
	void unreadableJpegFailsFastWithoutMagick() {
		byte[] brokenJpeg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2, 3, 4, 5, 6, 7, 8 };

		assertThatThrownBy(() -> converter().toDisplayJpeg(brokenJpeg, DetectedImageType.JPEG))
				.isInstanceOf(ImageConversionException.class)
				.hasMessageContaining("dönüştürülemedi");
	}

	@Test
	void heicWithoutMagickFailsFast() {
		assertThatThrownBy(() -> converter().toDisplayJpeg(TestImages.heicHeader(), DetectedImageType.HEIC))
				.isInstanceOf(ImageConversionException.class)
				.hasMessageContaining("ImageMagick");
	}

	private static ImageMagickImageConverter converter() {
		AppProperties properties = new AppProperties();
		properties.getImageMagick().setCommand("magick-does-not-exist");
		return new ImageMagickImageConverter(properties);
	}

	private static int sample(BufferedImage image, double xRatio, double yRatio) {
		int x = Math.min(image.getWidth() - 1, Math.max(0, (int) Math.round((image.getWidth() - 1) * xRatio)));
		int y = Math.min(image.getHeight() - 1, Math.max(0, (int) Math.round((image.getHeight() - 1) * yRatio)));
		return image.getRGB(x, y);
	}

	private static byte[] pngHalves(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setColor(Color.RED);
			graphics.fillRect(0, 0, width / 2, height);
			graphics.setColor(Color.BLUE);
			graphics.fillRect(width / 2, 0, width - width / 2, height);
		}
		finally {
			graphics.dispose();
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ImageIO.write(image, "png", buffer);
		return buffer.toByteArray();
	}
}
