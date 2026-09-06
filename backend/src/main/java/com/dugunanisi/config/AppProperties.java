package com.dugunanisi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private final Cors cors = new Cors();
	private final Admin admin = new Admin();
	private final Supabase supabase = new Supabase();
	private final ImageMagick imageMagick = new ImageMagick();
	private final RateLimit rateLimit = new RateLimit();
	private final ClientIp clientIp = new ClientIp();

	public Cors getCors() {
		return cors;
	}

	public Admin getAdmin() {
		return admin;
	}

	public Supabase getSupabase() {
		return supabase;
	}

	public ImageMagick getImageMagick() {
		return imageMagick;
	}

	public RateLimit getRateLimit() {
		return rateLimit;
	}

	public ClientIp getClientIp() {
		return clientIp;
	}

	public static class Cors {
		private String allowedOrigins = "http://localhost:5173";

		public String getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(String allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
		}

		public java.util.List<String> allowedOriginList() {
			if (allowedOrigins == null || allowedOrigins.isBlank()) {
				return java.util.List.of("http://localhost:5173");
			}
			return java.util.Arrays.stream(allowedOrigins.split(","))
					.map(String::trim)
					.filter(origin -> !origin.isEmpty() && !"*".equals(origin))
					.toList();
		}
	}

	public static class Admin {
		private String token = "";

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
	}

	public static class Supabase {
		private String url = "";
		private String serviceRoleKey = "";
		private String storageBucket = "guest-photos";
		private String publicStorageBaseUrl = "";

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getServiceRoleKey() {
			return serviceRoleKey;
		}

		public void setServiceRoleKey(String serviceRoleKey) {
			this.serviceRoleKey = serviceRoleKey;
		}

		public String getStorageBucket() {
			return storageBucket;
		}

		public void setStorageBucket(String storageBucket) {
			this.storageBucket = storageBucket;
		}

		public String getPublicStorageBaseUrl() {
			return publicStorageBaseUrl;
		}

		public void setPublicStorageBaseUrl(String publicStorageBaseUrl) {
			this.publicStorageBaseUrl = publicStorageBaseUrl;
		}

		public String publicObjectUrl(String objectPath) {
			String normalized = objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
			if (publicStorageBaseUrl != null && !publicStorageBaseUrl.isBlank()) {
				return trimSlash(publicStorageBaseUrl) + "/" + normalized;
			}
			return trimSlash(url) + "/storage/v1/object/public/" + storageBucket + "/" + normalized;
		}

		private static String trimSlash(String value) {
			if (value == null || value.isBlank()) {
				return "";
			}
			return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
		}
	}

	public static class ImageMagick {
		private String command = "magick";

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}
	}

	public static class RateLimit {
		private int photoUploadsPerWindow = 60;
		private int memoryPostsPerWindow = 20;
		private java.time.Duration window = java.time.Duration.ofMinutes(10);

		public int getPhotoUploadsPerWindow() {
			return photoUploadsPerWindow;
		}

		public void setPhotoUploadsPerWindow(int photoUploadsPerWindow) {
			this.photoUploadsPerWindow = photoUploadsPerWindow;
		}

		public int getMemoryPostsPerWindow() {
			return memoryPostsPerWindow;
		}

		public void setMemoryPostsPerWindow(int memoryPostsPerWindow) {
			this.memoryPostsPerWindow = memoryPostsPerWindow;
		}

		public java.time.Duration getWindow() {
			return window;
		}

		public void setWindow(java.time.Duration window) {
			this.window = window;
		}
	}

	public static class ClientIp {
		private boolean trustForwardedHeaders = false;
		private String trustedProxies = "127.0.0.1,::1";

		public boolean isTrustForwardedHeaders() {
			return trustForwardedHeaders;
		}

		public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
			this.trustForwardedHeaders = trustForwardedHeaders;
		}

		public String getTrustedProxies() {
			return trustedProxies;
		}

		public void setTrustedProxies(String trustedProxies) {
			this.trustedProxies = trustedProxies;
		}

		public java.util.List<String> trustedProxyList() {
			if (trustedProxies == null || trustedProxies.isBlank()) {
				return java.util.List.of();
			}
			return java.util.Arrays.stream(trustedProxies.split(","))
					.map(String::trim)
					.filter(value -> !value.isEmpty())
					.toList();
		}
	}
}
