package com.dugunanisi.image;

/**
 * Reads TIFF Orientation (tag 0x0112) from a JPEG APP1 Exif segment.
 * ImageIO does not apply this tag, so phone photos would otherwise stay
 * in sensor orientation when written to {@code display.jpg}.
 */
final class JpegExifOrientation {

	static final int NORMAL = 1;

	private static final int SOI = 0xD8;
	private static final int SOS = 0xDA;
	private static final int EOI = 0xD9;
	private static final int APP1 = 0xE1;
	private static final int ORIENTATION_TAG = 0x0112;

	private JpegExifOrientation() {
	}

	static int read(byte[] jpeg) {
		if (jpeg == null || jpeg.length < 4
				|| (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != SOI) {
			return NORMAL;
		}
		int index = 2;
		while (index + 3 < jpeg.length) {
			if ((jpeg[index] & 0xFF) != 0xFF) {
				return NORMAL;
			}
			while (index < jpeg.length && (jpeg[index] & 0xFF) == 0xFF) {
				index++;
			}
			if (index >= jpeg.length) {
				return NORMAL;
			}
			int marker = jpeg[index] & 0xFF;
			index++;
			if (marker == SOS || marker == EOI || marker == SOI) {
				return NORMAL;
			}
			if (marker >= 0xD0 && marker <= 0xD7) {
				continue;
			}
			if (index + 1 >= jpeg.length) {
				return NORMAL;
			}
			int segmentLength = ((jpeg[index] & 0xFF) << 8) | (jpeg[index + 1] & 0xFF);
			if (segmentLength < 2 || index + segmentLength > jpeg.length) {
				return NORMAL;
			}
			if (marker == APP1) {
				int orientation = readExifOrientation(jpeg, index + 2, segmentLength - 2);
				if (orientation != NORMAL) {
					return orientation;
				}
			}
			index += segmentLength;
		}
		return NORMAL;
	}

	private static int readExifOrientation(byte[] jpeg, int offset, int length) {
		if (length < 14) {
			return NORMAL;
		}
		if (!(jpeg[offset] == 'E' && jpeg[offset + 1] == 'x' && jpeg[offset + 2] == 'i'
				&& jpeg[offset + 3] == 'f' && jpeg[offset + 4] == 0 && jpeg[offset + 5] == 0)) {
			return NORMAL;
		}
		int tiff = offset + 6;
		boolean littleEndian;
		if (jpeg[tiff] == 'I' && jpeg[tiff + 1] == 'I') {
			littleEndian = true;
		}
		else if (jpeg[tiff] == 'M' && jpeg[tiff + 1] == 'M') {
			littleEndian = false;
		}
		else {
			return NORMAL;
		}
		if (u16(jpeg, tiff + 2, littleEndian) != 0x002A) {
			return NORMAL;
		}
		long ifdOffset = u32(jpeg, tiff + 4, littleEndian);
		int ifd = tiff + (int) ifdOffset;
		if (ifdOffset > Integer.MAX_VALUE - tiff || ifd + 2 > offset + length) {
			return NORMAL;
		}
		int entries = u16(jpeg, ifd, littleEndian);
		int cursor = ifd + 2;
		for (int i = 0; i < entries; i++) {
			if (cursor + 12 > offset + length) {
				return NORMAL;
			}
			int tag = u16(jpeg, cursor, littleEndian);
			int type = u16(jpeg, cursor + 2, littleEndian);
			long count = u32(jpeg, cursor + 4, littleEndian);
			if (tag == ORIENTATION_TAG && count == 1) {
				int value = type == 3
						? u16(jpeg, cursor + 8, littleEndian)
						: (int) u32(jpeg, cursor + 8, littleEndian);
				return value >= 1 && value <= 8 ? value : NORMAL;
			}
			cursor += 12;
		}
		return NORMAL;
	}

	private static int u16(byte[] bytes, int offset, boolean littleEndian) {
		int a = bytes[offset] & 0xFF;
		int b = bytes[offset + 1] & 0xFF;
		return littleEndian ? a | (b << 8) : (a << 8) | b;
	}

	private static long u32(byte[] bytes, int offset, boolean littleEndian) {
		long a = bytes[offset] & 0xFF;
		long b = bytes[offset + 1] & 0xFF;
		long c = bytes[offset + 2] & 0xFF;
		long d = bytes[offset + 3] & 0xFF;
		return littleEndian ? a | (b << 8) | (c << 16) | (d << 24) : (a << 24) | (b << 16) | (c << 8) | d;
	}
}
