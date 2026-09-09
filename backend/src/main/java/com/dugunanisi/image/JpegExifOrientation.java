package com.dugunanisi.image;

import java.io.ByteArrayInputStream;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

/**
 * Reads TIFF Orientation (tag 0x0112) from a JPEG.
 * ImageIO does not apply this tag, so phone photos would otherwise stay
 * in sensor orientation when written to {@code display.jpg}.
 * <p>
 * iPhone JPEGs often have a large APP1 with many IFD0 tags, GPS/Exif pointers,
 * a thumbnail IFD, and sometimes padding before the TIFF header. A one-entry
 * synthetic APP1 is not enough; this reader uses metadata-extractor first and
 * falls back to a segment walker.
 */
final class JpegExifOrientation {

	static final int NORMAL = 1;

	private static final int SOI = 0xD8;
	private static final int SOS = 0xDA;
	private static final int EOI = 0xD9;
	private static final int APP1 = 0xE1;
	private static final int ORIENTATION_TAG = 0x0112;
	private static final int EXIF_SUBIFD_TAG = 0x8769;

	private JpegExifOrientation() {
	}

	static int read(byte[] jpeg) {
		try {
			int fromLibrary = readWithMetadataExtractor(jpeg);
			if (fromLibrary != NORMAL) {
				return fromLibrary;
			}
			return readFromApp1(jpeg);
		}
		catch (Throwable ignored) {
			// Canvas-reencoded JPEGs often have no EXIF. Missing/odd metadata is orientation 1.
			return NORMAL;
		}
	}

	private static int readWithMetadataExtractor(byte[] jpeg) {
		if (jpeg == null || jpeg.length < 4) {
			return NORMAL;
		}
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(jpeg));
			int ifd0 = orientationFrom(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class));
			if (ifd0 != NORMAL) {
				return ifd0;
			}
			return orientationFrom(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class));
		}
		catch (Exception ignored) {
			return NORMAL;
		}
	}

	private static int orientationFrom(com.drew.metadata.Directory directory) {
		if (directory == null || !directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
			return NORMAL;
		}
		try {
			int value = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			return value >= 1 && value <= 8 ? value : NORMAL;
		}
		catch (Exception ignored) {
			return NORMAL;
		}
	}

	static int readFromApp1(byte[] jpeg) {
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
		int exifAt = indexOfExifHeader(jpeg, offset, length);
		if (exifAt < 0) {
			return NORMAL;
		}
		int tiff = findTiffHeader(jpeg, exifAt + 6, offset + length);
		if (tiff < 0) {
			return NORMAL;
		}
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
		if (tiff + 8 > offset + length || u16(jpeg, tiff + 2, littleEndian) != 0x002A) {
			return NORMAL;
		}
		int limit = offset + length;
		long ifdOffset = u32(jpeg, tiff + 4, littleEndian);
		int orientation = readOrientationInIfd(jpeg, tiff, ifdOffset, limit, littleEndian);
		if (orientation != NORMAL) {
			return orientation;
		}
		long subIfd = readPointerTag(jpeg, tiff, ifdOffset, limit, littleEndian, EXIF_SUBIFD_TAG);
		if (subIfd > 0) {
			return readOrientationInIfd(jpeg, tiff, subIfd, limit, littleEndian);
		}
		return NORMAL;
	}

	private static int indexOfExifHeader(byte[] jpeg, int offset, int length) {
		int end = Math.min(offset + length - 5, offset + 32);
		for (int i = offset; i <= end; i++) {
			if (jpeg[i] == 'E' && jpeg[i + 1] == 'x' && jpeg[i + 2] == 'i'
					&& jpeg[i + 3] == 'f' && jpeg[i + 4] == 0 && jpeg[i + 5] == 0) {
				return i;
			}
		}
		return -1;
	}

	private static int findTiffHeader(byte[] jpeg, int from, int limit) {
		int end = Math.min(from + 16, limit - 1);
		for (int i = from; i < end; i++) {
			if ((jpeg[i] == 'I' && jpeg[i + 1] == 'I') || (jpeg[i] == 'M' && jpeg[i + 1] == 'M')) {
				return i;
			}
		}
		return -1;
	}

	private static int readOrientationInIfd(
			byte[] jpeg,
			int tiff,
			long ifdOffset,
			int limit,
			boolean littleEndian) {
		int ifd = tiff + (int) ifdOffset;
		if (ifdOffset > Integer.MAX_VALUE - tiff || ifd + 2 > limit) {
			return NORMAL;
		}
		int entries = u16(jpeg, ifd, littleEndian);
		int cursor = ifd + 2;
		for (int i = 0; i < entries; i++) {
			if (cursor + 12 > limit) {
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

	private static long readPointerTag(
			byte[] jpeg,
			int tiff,
			long ifdOffset,
			int limit,
			boolean littleEndian,
			int wantedTag) {
		int ifd = tiff + (int) ifdOffset;
		if (ifdOffset > Integer.MAX_VALUE - tiff || ifd + 2 > limit) {
			return 0;
		}
		int entries = u16(jpeg, ifd, littleEndian);
		int cursor = ifd + 2;
		for (int i = 0; i < entries; i++) {
			if (cursor + 12 > limit) {
				return 0;
			}
			int tag = u16(jpeg, cursor, littleEndian);
			long count = u32(jpeg, cursor + 4, littleEndian);
			if (tag == wantedTag && count == 1) {
				return u32(jpeg, cursor + 8, littleEndian);
			}
			cursor += 12;
		}
		return 0;
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
