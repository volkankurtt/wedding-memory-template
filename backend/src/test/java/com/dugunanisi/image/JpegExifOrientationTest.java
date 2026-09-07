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
	void readsIphoneStyleApp1WithXmpPaddingAndManyIfdTags() throws Exception {
		byte[] original = OrientedJpegs.solidHalves(64, 32);
		byte[] iphone = OrientedJpegs.withIphoneStyleOrientation(original, 6);
		assertThat(JpegExifOrientation.read(iphone)).isEqualTo(6);
		assertThat(JpegExifOrientation.readFromApp1(iphone)).isEqualTo(6);
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
		return insertAfterSoi(jpeg, exifApp1(orientation, littleEndian, 0, false));
	}

	static byte[] withIphoneStyleOrientation(byte[] jpeg, int orientation) {
		byte[] xmp = xmpApp1();
		byte[] exif = exifApp1(orientation, true, 4, true);
		byte[] prefix = new byte[xmp.length + exif.length];
		System.arraycopy(xmp, 0, prefix, 0, xmp.length);
		System.arraycopy(exif, 0, prefix, xmp.length, exif.length);
		return insertAfterSoi(jpeg, prefix);
	}

	private static byte[] insertAfterSoi(byte[] jpeg, byte[] segment) {
		byte[] out = new byte[jpeg.length + segment.length];
		out[0] = jpeg[0];
		out[1] = jpeg[1];
		System.arraycopy(segment, 0, out, 2, segment.length);
		System.arraycopy(jpeg, 2, out, 2 + segment.length, jpeg.length - 2);
		return out;
	}

	private static byte[] xmpApp1() {
		byte[] payload = "http://ns.adobe.com/xap/1.0/\0<x:xmpmeta/>".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
		byte[] app1 = new byte[4 + payload.length];
		app1[0] = (byte) 0xFF;
		app1[1] = (byte) 0xE1;
		int length = payload.length + 2;
		app1[2] = (byte) ((length >> 8) & 0xFF);
		app1[3] = (byte) (length & 0xFF);
		System.arraycopy(payload, 0, app1, 4, payload.length);
		return app1;
	}

	private static byte[] exifApp1(int orientation, boolean littleEndian, int paddingBeforeTiff, boolean manyTags) {
		int extraTags = manyTags ? 6 : 0;
		int entries = 1 + extraTags;
		int tiffSize = 8 + 2 + entries * 12 + 4;
		byte[] tiff = new byte[paddingBeforeTiff + tiffSize];
		int tiffStart = paddingBeforeTiff;
		if (littleEndian) {
			tiff[tiffStart] = 'I';
			tiff[tiffStart + 1] = 'I';
			tiff[tiffStart + 2] = 0x2A;
			tiff[tiffStart + 3] = 0x00;
			putU32(tiff, tiffStart + 4, 8, true);
		}
		else {
			tiff[tiffStart] = 'M';
			tiff[tiffStart + 1] = 'M';
			tiff[tiffStart + 2] = 0x00;
			tiff[tiffStart + 3] = 0x2A;
			putU32(tiff, tiffStart + 4, 8, false);
		}
		putU16(tiff, tiffStart + 8, entries, littleEndian);
		int cursor = tiffStart + 10;
		int[] dummyTags = { 0x0100, 0x0101, 0x0103, 0x0106, 0x0115, 0x011C };
		for (int i = 0; i < extraTags; i++) {
			putU16(tiff, cursor, dummyTags[i], littleEndian);
			putU16(tiff, cursor + 2, 3, littleEndian);
			putU32(tiff, cursor + 4, 1, littleEndian);
			putU16(tiff, cursor + 8, 1, littleEndian);
			cursor += 12;
		}
		putU16(tiff, cursor, 0x0112, littleEndian);
		putU16(tiff, cursor + 2, 3, littleEndian);
		putU32(tiff, cursor + 4, 1, littleEndian);
		putU16(tiff, cursor + 8, orientation, littleEndian);
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
