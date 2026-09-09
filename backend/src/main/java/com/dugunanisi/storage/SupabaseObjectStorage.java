package com.dugunanisi.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dugunanisi.config.AppProperties;

@Component
public class SupabaseObjectStorage implements ObjectStorage {

	private static final Logger log = LoggerFactory.getLogger(SupabaseObjectStorage.class);

	private final RestClient restClient;
	private final AppProperties.Supabase supabase;

	public SupabaseObjectStorage(RestClient.Builder restClientBuilder, AppProperties properties) {
		this.restClient = restClientBuilder.build();
		this.supabase = properties.getSupabase();
	}

	@Override
	public void put(String objectPath, byte[] bytes, String contentType) {
		assertConfigured();
		long started = System.nanoTime();
		log.info("Storage PUT start path={} bytes={} type={} host={}",
				objectPath, bytes.length, contentType, hostOf(objectUri(objectPath)));
		try {
			restClient.put()
					.uri(objectUri(objectPath))
					.headers(this::applyServiceRole)
					.header("x-upsert", "true")
					.contentType(parseMediaType(contentType))
					.body(bytes)
					.retrieve()
					.toBodilessEntity();
			log.info("Storage PUT done path={} bytes={} ms={}", objectPath, bytes.length, elapsedMs(started));
		}
		catch (RestClientResponseException exception) {
			log.warn("Storage PUT failed path={} status={} body={} ms={}",
					objectPath,
					exception.getStatusCode().value(),
					exception.getResponseBodyAsString(),
					elapsedMs(started));
			throw new StorageException("Fotoğraf depolanamadı.", exception);
		}
		catch (RestClientException exception) {
			log.warn("Storage PUT failed path={} ms={}: {}", objectPath, elapsedMs(started), exception.toString());
			throw new StorageException("Fotoğraf depolanamadı.", exception);
		}
	}

	@Override
	public boolean exists(String objectPath) {
		assertConfigured();
		try {
			restClient.head()
					.uri(objectUri(objectPath))
					.headers(this::applyServiceRole)
					.retrieve()
					.toBodilessEntity();
			return true;
		}
		catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 404) {
				return false;
			}
			log.warn("Storage HEAD failed path={} status={}, assuming missing",
					objectPath, exception.getStatusCode().value());
			return false;
		}
		catch (RestClientException exception) {
			throw new StorageException("Depolama kontrolü başarısız.", exception);
		}
	}

	@Override
	public byte[] get(String objectPath) {
		assertConfigured();
		long started = System.nanoTime();
		log.info("Storage GET start path={} host={}", objectPath, hostOf(objectUri(objectPath)));
		try {
			byte[] body = restClient.get()
					.uri(objectUri(objectPath))
					.headers(this::applyServiceRole)
					.retrieve()
					.body(byte[].class);
			if (body == null) {
				throw new StorageException("Fotoğraf depoda bulunamadı.");
			}
			log.info("Storage GET done path={} bytes={} ms={}", objectPath, body.length, elapsedMs(started));
			return body;
		}
		catch (RestClientResponseException exception) {
			log.warn("Storage GET failed path={} status={} ms={}",
					objectPath, exception.getStatusCode().value(), elapsedMs(started));
			if (exception.getStatusCode().value() == 404) {
				throw new StorageException("Fotoğraf depoda bulunamadı.", exception);
			}
			throw new StorageException("Fotoğraf okunamadı.", exception);
		}
		catch (RestClientException exception) {
			log.warn("Storage GET failed path={} ms={}: {}", objectPath, elapsedMs(started), exception.toString());
			throw new StorageException("Fotoğraf okunamadı.", exception);
		}
	}

	@Override
	public SignedUpload createSignedUpload(String objectPath, String contentType, Duration ttl) {
		assertConfigured();
		long started = System.nanoTime();
		String signUri = signUploadUri(objectPath);
		log.info("signedUpload start path={} host={}", objectPath, hostOf(signUri));
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> body = restClient.post()
					.uri(signUri)
					.headers(this::applyServiceRole)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of())
					.retrieve()
					.body(Map.class);
			if (body == null || body.get("url") == null) {
				throw new StorageException("Yükleme adresi üretilemedi.");
			}
			String rawUrl = String.valueOf(body.get("url"));
			String signedUrl = StorageHostnames.rewriteToDedicatedStorage(toAbsoluteStorageUrl(rawUrl));
			String token = tokenFrom(signedUrl, body.get("token"));
			if (token == null || token.isBlank()) {
				throw new StorageException("Yükleme adresi üretilemedi.");
			}
			log.info("signedUpload done path={} host={} ms={}", objectPath, hostOf(signedUrl), elapsedMs(started));
			return new SignedUpload(signedUrl, token, 2 * 60 * 60);
		}
		catch (StorageException exception) {
			throw exception;
		}
		catch (RestClientException exception) {
			log.warn("Signed upload create failed path={}: {}", objectPath, exception.toString());
			throw new StorageException("Yükleme adresi üretilemedi.", exception);
		}
	}

	@Override
	public void delete(String objectPath) {
		assertConfigured();
		try {
			restClient.delete()
					.uri(objectUri(objectPath))
					.headers(this::applyServiceRole)
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientException exception) {
			throw new StorageException("Fotoğraf depodan silinemedi.", exception);
		}
	}

	private static MediaType parseMediaType(String contentType) {
		try {
			return MediaType.parseMediaType(contentType);
		}
		catch (Exception exception) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	private void applyServiceRole(org.springframework.http.HttpHeaders headers) {
		headers.setBearerAuth(supabase.getServiceRoleKey());
		headers.set("apikey", supabase.getServiceRoleKey());
	}

	private void assertConfigured() {
		if (supabase.getUrl() == null || supabase.getUrl().isBlank()
				|| supabase.getServiceRoleKey() == null || supabase.getServiceRoleKey().isBlank()) {
			throw new StorageException("Depolama yapılandırılmadı.");
		}
	}

	private static long elapsedMs(long startedNanos) {
		return (System.nanoTime() - startedNanos) / 1_000_000L;
	}

	private String objectUri(String objectPath) {
		return storageBase() + "/object/" + supabase.getStorageBucket() + "/" + objectPath;
	}

	private String signUploadUri(String objectPath) {
		return storageBase() + "/object/upload/sign/" + supabase.getStorageBucket() + "/" + objectPath;
	}

	private String storageBase() {
		return StorageHostnames.dedicatedStorageOrigin(supabase.getUrl()) + "/storage/v1";
	}

	private String toAbsoluteStorageUrl(String rawUrl) {
		if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
			return rawUrl;
		}
		if (rawUrl.startsWith("/")) {
			return storageBase() + rawUrl;
		}
		return storageBase() + "/" + rawUrl;
	}

	private static String hostOf(String url) {
		try {
			String host = URI.create(url).getHost();
			return host == null ? "" : host;
		}
		catch (IllegalArgumentException exception) {
			return "";
		}
	}

	private static String tokenFrom(String signedUrl, Object tokenField) {
		if (tokenField != null && !String.valueOf(tokenField).isBlank()) {
			return String.valueOf(tokenField);
		}
		try {
			String token = URI.create(signedUrl).getQuery();
			if (token == null) {
				return null;
			}
			for (String part : token.split("&")) {
				int eq = part.indexOf('=');
				if (eq > 0 && "token".equals(part.substring(0, eq))) {
					return part.substring(eq + 1);
				}
			}
		}
		catch (IllegalArgumentException ignored) {
			return null;
		}
		return null;
	}

	private static String trimSlash(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
