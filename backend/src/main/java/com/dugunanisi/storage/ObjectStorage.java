package com.dugunanisi.storage;

import java.time.Duration;

public interface ObjectStorage {

	void put(String objectPath, byte[] bytes, String contentType);

	void delete(String objectPath);

	boolean exists(String objectPath);

	byte[] get(String objectPath);

	SignedUpload createSignedUpload(String objectPath, String contentType, Duration ttl);
}
