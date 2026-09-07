package com.dugunanisi.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class JpegExifOrientationTest {

	@Test
	void readsBigEndianAndLittleEndianOrientation() throws Exception {
		byte[] landscape = OrientedJpegs.solidHalves(64, 32);
		assertThat(JpegExifOrientation.read(landscape)).isEqualTo(1);
		assertThat(JpegExifOrientation.read(OrientedJpegs.withOrientation(landscape, 6, true))).isEqualTo(6);
		assertThat(JpegExifOrientation.read(OrientedJpegs.withOrientation(landscape, 8, false))).isEqualTo(8);
	}

	@Test
	void ignoresNonJpegAndMissingExif() {
		assertThat(JpegExifOrientation.read(new byte[] { 1, 2, 3 })).isEqualTo(1);
		assertThat(JpegExifOrientation.read(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF })).isEqualTo(1);
	}
}

final class OrientedJpegs {

	private OrientedJpegs() {
	}

	static byte[] solidHalves(int width, int height) throws IOException {
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
		if (!ImageIO.write(image, "jpg", buffer)) {
			throw new IOException("Could not write test jpeg");
		}
		return buffer.toByteArray();
	}

	static byte[] withOrientation(byte[] jpeg, int orientation, boolean littleEndian) {
		byte[] app1 = exifApp1(orientation, littleEndian);
		byte[] out = new byte[jpeg.length + app1.length];
		out[0] = jpeg[0];
		out[1] = jpeg[1];
		System.arraycopy(app1, 0, out, 2, app1.length);
		System.arraycopy(jpeg, 2, out, 2 + app1.length, jpeg.length - 2);
		return out;
	}

	private static byte[] exifApp1(int orientation, boolean littleEndian) {
		byte[] tiff = new byte[26];
		if (littleEndian) {
			tiff[0] = 'I';
			tiff[1] = 'I';
			tiff[2] = 0x2A;
			tiff[3] = 0x00;
			tiff[4] = 0x08;
		}
		else {
			tiff[0] = 'M';
			tiff[1] = 'M';
			tiff[2] = 0x00;
			tiff[3] = 0x2A;
			tiff[7] = 0x08;
		}
		putU16(tiff, 8, 1, littleEndian);
		putU16(tiff, 10, 0x0112, littleEndian);
		putU16(tiff, 12, 3, littleEndian);
		putU32(tiff, 14, 1, littleEndian);
		putU16(tiff, 18, orientation, littleEndian);
		byte[] app1 = new byte[2 + 2 + 6 + tiff.length];
		app1[0] = (byte) 0xFF;
		app1[1] = (byte) 0xE1;
		int length = app1.length - 2;
		app1[2] = (byte) ((length >> 8) & 0xFF);
		app1[3] = (byte) (length & 0xFF);
		app1[4] = 'E';
		app1[5] = 'x';
		app1[6] = 'i';
		app1[7] = 'f';
		System.arraycopy(tiff, 0, app1, 10, tiff.length);
		return app1;
	}

	private static void putU16(byte[] bytes, int offset, int value, boolean littleEndian) {
		if (littleEndian) {
			bytes[offset] = (byte) (value & 0xFF);
			bytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
		}
		else {
			bytes[offset] = (byte) ((value >> 8) & 0xFF);
			bytes[offset + 1] = (byte) (value & 0xFF);
		}
	}

	private static void putU32(byte[] bytes, int offset, long value, boolean littleEndian) {
		if (littleEndian) {
			bytes[offset] = (byte) (value & 0xFF);
			bytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
			bytes[offset + 2] = (byte) ((value >> 16) & 0xFF);
			bytes[offset + 3] = (byte) ((value >> 24) & 0xFF);
		}
		else {
			bytes[offset] = (byte) ((value >> 24) & 0xFF);
			bytes[offset + 1] = (byte) ((value >> 16) & 0xFF);
			bytes[offset + 2] = (byte) ((value >> 8) & 0xFF);
			bytes[offset + 3] = (byte) (value & 0xFF);
		}
	}

	static BufferedImage read(byte[] jpeg) throws IOException {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
		if (image == null) {
			throw new IOException("Could not read jpeg");
		}
		return image;
	}

	static boolean reddish(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return r > 160 && g < 90 && b < 90;
	}

	static boolean bluish(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return b > 160 && r < 90 && g < 90;
	}
}
