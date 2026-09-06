package com.dugunanisi.ratelimit;

import java.util.List;

import com.dugunanisi.config.AppProperties;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpResolver {

	private final AppProperties.ClientIp settings;

	public ClientIpResolver(AppProperties properties) {
		this.settings = properties.getClientIp();
	}

	public String resolve(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		if (remoteAddr == null || remoteAddr.isBlank()) {
			return "unknown";
		}
		if (!settings.isTrustForwardedHeaders()) {
			return remoteAddr;
		}
		if (!isTrustedProxy(remoteAddr, settings.trustedProxyList())) {
			return remoteAddr;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded == null || forwarded.isBlank()) {
			return remoteAddr;
		}
		String client = forwarded.split(",")[0].trim();
		return client.isEmpty() ? remoteAddr : client;
	}

	static boolean isTrustedProxy(String remoteAddr, List<String> trusted) {
		return trusted.stream().anyMatch(remoteAddr::equals);
	}
}
