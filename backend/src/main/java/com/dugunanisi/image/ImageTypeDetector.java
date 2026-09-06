package com.dugunanisi.image;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ImageTypeDetector {

	private static final Set<String> ALLOWED_MIME = Set.of(
			"image/jpeg",
			"image/jpg",
			"image/pjpeg",
			"image/png",
			"image/webp",
			"image/heic",
			"image/heif",
			"image/heic-sequence",
			"image/heif-sequence",
			"application/octet-stream");

	private static final Set<String> HEIF_BRANDS = Set.of(
			"heic", "heif", "heix", "heim", "heis", "hevc", "hevx", "mif1", "msf1");

	public DetectedImageType detect(byte[] bytes) {
		if (bytes == null || bytes.length < 12) {
			return null;
		}
		if (isJpeg(bytes)) {
			return DetectedImageType.JPEG;
		}
		if (isPng(bytes)) {
			return DetectedImageType.PNG;
		}
		if (isWebp(bytes)) {
			return DetectedImageType.WEBP;
		}
		if (isHeif(bytes)) {
			return DetectedImageType.HEIC;
		}
		return null;
	}

	public boolean isAllowedMime(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return true;
		}
		String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		return ALLOWED_MIME.contains(normalized);
	}

	private static boolean isJpeg(byte[] bytes) {
		return bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
	}

	private static boolean isPng(byte[] bytes) {
		return bytes[0] == (byte) 0x89
				&& bytes[1] == 0x50
				&& bytes[2] == 0x4E
				&& bytes[3] == 0x47
				&& bytes[4] == 0x0D
				&& bytes[5] == 0x0A
				&& bytes[6] == 0x1A
				&& bytes[7] == 0x0A;
	}

	private static boolean isWebp(byte[] bytes) {
		return ascii(bytes, 0, 4).equals("RIFF") && ascii(bytes, 8, 4).equals("WEBP");
	}

	private static boolean isHeif(byte[] bytes) {
		if (bytes.length < 16 || !ascii(bytes, 4, 4).equals("ftyp")) {
			return false;
		}
		String major = ascii(bytes, 8, 4);
		if (HEIF_BRANDS.contains(major)) {
			return true;
		}
		int boxSize = readInt(bytes, 0);
		int limit = Math.min(bytes.length, boxSize > 8 ? boxSize : bytes.length);
		for (int offset = 16; offset + 4 <= limit; offset += 4) {
			if (HEIF_BRANDS.contains(ascii(bytes, offset, 4))) {
				return true;
			}
		}
		return false;
	}

	private static String ascii(byte[] bytes, int offset, int length) {
		return new String(bytes, offset, length, StandardCharsets.US_ASCII);
	}

	private static int readInt(byte[] bytes, int offset) {
		return ((bytes[offset] & 0xFF) << 24)
				| ((bytes[offset + 1] & 0xFF) << 16)
				| ((bytes[offset + 2] & 0xFF) << 8)
				| (bytes[offset + 3] & 0xFF);
	}
}
