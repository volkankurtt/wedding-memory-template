package com.dugunanisi.storage;

import java.net.URI;

/**
 * Supabase documents the dedicated storage host
 * {@code https://&lt;project-ref&gt;.storage.supabase.co} for faster uploads
 * than the API gateway {@code https://&lt;project-ref&gt;.supabase.co}.
 */
public final class StorageHostnames {

	private StorageHostnames() {
	}

	public static String dedicatedStorageOrigin(String supabaseUrl) {
		if (supabaseUrl == null || supabaseUrl.isBlank()) {
			return supabaseUrl;
		}
		try {
			URI uri = URI.create(supabaseUrl.trim());
			String rewritten = rewriteUri(uri);
			return trimSlash(rewritten != null ? originOf(URI.create(rewritten)) : trimSlash(supabaseUrl));
		}
		catch (IllegalArgumentException exception) {
			return trimSlash(supabaseUrl);
		}
	}

	public static String rewriteToDedicatedStorage(String url) {
		if (url == null || url.isBlank()) {
			return url;
		}
		try {
			String rewritten = rewriteUri(URI.create(url.trim()));
			return rewritten != null ? rewritten : url;
		}
		catch (IllegalArgumentException exception) {
			return url;
		}
	}

	private static String rewriteUri(URI uri) {
		String host = uri.getHost();
		if (host == null || !host.matches("(?i)[a-z0-9-]+\\.supabase\\.co")) {
			return null;
		}
		String dedicated = host.replaceFirst("(?i)\\.supabase\\.co$", ".storage.supabase.co");
		try {
			return new URI(
					uri.getScheme(),
					uri.getUserInfo(),
					dedicated,
					uri.getPort(),
					uri.getPath(),
					uri.getQuery(),
					uri.getFragment()).toString();
		}
		catch (Exception exception) {
			return null;
		}
	}

	private static String originOf(URI uri) {
		int port = uri.getPort();
		String origin = uri.getScheme() + "://" + uri.getHost();
		if (port > 0) {
			origin += ":" + port;
		}
		return origin;
	}

	private static String trimSlash(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
