package com.dugunanisi.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryObjectStorage implements ObjectStorage {

	private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

	@Override
	public void put(String objectPath, byte[] bytes, String contentType) {
		objects.put(objectPath, bytes);
	}

	@Override
	public void delete(String objectPath) {
		objects.remove(objectPath);
	}

	public byte[] get(String objectPath) {
		return objects.get(objectPath);
	}

	public boolean contains(String objectPath) {
		return objects.containsKey(objectPath);
	}
}
