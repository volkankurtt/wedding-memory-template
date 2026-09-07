package com.dugunanisi.storage;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryObjectStorage implements ObjectStorage {

	private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
	private final AtomicInteger tokens = new AtomicInteger();

	@Override
	public void put(String objectPath, byte[] bytes, String contentType) {
		objects.put(objectPath, bytes);
	}

	@Override
	public void delete(String objectPath) {
		objects.remove(objectPath);
	}

	@Override
	public boolean exists(String objectPath) {
		return objects.containsKey(objectPath);
	}

	@Override
	public byte[] get(String objectPath) {
		byte[] bytes = objects.get(objectPath);
		if (bytes == null) {
			throw new StorageException("Fotoğraf depoda bulunamadı.");
		}
		return bytes;
	}

	@Override
	public SignedUpload createSignedUpload(String objectPath, String contentType, Duration ttl) {
		String token = "test-token-" + tokens.incrementAndGet();
		return new SignedUpload("https://example.supabase.co/storage/v1/object/upload/sign/guest-photos/"
				+ objectPath + "?token=" + token, token, 2 * 60 * 60);
	}

	public void clear() {
		objects.clear();
	}
}
