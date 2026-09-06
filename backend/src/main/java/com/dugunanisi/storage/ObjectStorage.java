package com.dugunanisi.storage;

public interface ObjectStorage {

	void put(String objectPath, byte[] bytes, String contentType);

	void delete(String objectPath);
}
