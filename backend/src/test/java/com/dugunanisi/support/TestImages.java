package com.dugunanisi.support;

import java.util.Base64;

public final class TestImages {

	private TestImages() {
	}

	public static final byte[] JPEG = Base64.getDecoder().decode(
			"/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCwAA8A/9k=");

	public static final byte[] GIF = new byte[] { 'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00, 0x01, 0x00 };

	public static byte[] heicHeader() {
		byte[] bytes = new byte[24];
		bytes[3] = 24;
		bytes[4] = 'f';
		bytes[5] = 't';
		bytes[6] = 'y';
		bytes[7] = 'p';
		bytes[8] = 'h';
		bytes[9] = 'e';
		bytes[10] = 'i';
		bytes[11] = 'c';
		return bytes;
	}
}
