package com.dugunanisi.storage;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dugunanisi.config.AppProperties;

@Component
public class SupabaseObjectStorage implements ObjectStorage {

	private final RestClient restClient;
	private final AppProperties.Supabase supabase;

	public SupabaseObjectStorage(RestClient.Builder restClientBuilder, AppProperties properties) {
		this.restClient = restClientBuilder.build();
		this.supabase = properties.getSupabase();
	}

	@Override
	public void put(String objectPath, byte[] bytes, String contentType) {
		assertConfigured();
		try {
			restClient.put()
					.uri(objectUri(objectPath))
					.header("Authorization", "Bearer " + supabase.getServiceRoleKey())
					.header("apikey", supabase.getServiceRoleKey())
					.header("x-upsert", "true")
					.contentType(parseMediaType(contentType))
					.body(bytes)
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientResponseException exception) {
			throw new StorageException("Fotoğraf depolanamadı.", exception);
		}
		catch (RestClientException exception) {
			throw new StorageException("Fotoğraf depolanamadı.", exception);
		}
	}

	@Override
	public void delete(String objectPath) {
		assertConfigured();
		try {
			restClient.delete()
					.uri(objectUri(objectPath))
					.header("Authorization", "Bearer " + supabase.getServiceRoleKey())
					.header("apikey", supabase.getServiceRoleKey())
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

	private void assertConfigured() {
		if (supabase.getUrl() == null || supabase.getUrl().isBlank()
				|| supabase.getServiceRoleKey() == null || supabase.getServiceRoleKey().isBlank()) {
			throw new StorageException("Depolama yapılandırılmadı.");
		}
	}

	private String objectUri(String objectPath) {
		String base = supabase.getUrl().endsWith("/")
				? supabase.getUrl().substring(0, supabase.getUrl().length() - 1)
				: supabase.getUrl();
		return base + "/storage/v1/object/" + supabase.getStorageBucket() + "/" + objectPath;
	}
}
