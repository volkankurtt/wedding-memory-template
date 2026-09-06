package com.dugunanisi.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.dugunanisi.config.AppProperties;

import jakarta.servlet.http.HttpServletRequest;

class ClientIpResolverTest {

	@Test
	void ignoresForwardedHeaderWhenNotTrusted() {
		AppProperties properties = new AppProperties();
		properties.getClientIp().setTrustForwardedHeaders(false);
		ClientIpResolver resolver = new ClientIpResolver(properties);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("203.0.113.10");
		when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

		assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
	}

	@Test
	void ignoresForwardedHeaderWhenPeerIsNotTrustedProxy() {
		AppProperties properties = new AppProperties();
		properties.getClientIp().setTrustForwardedHeaders(true);
		properties.getClientIp().setTrustedProxies("10.0.0.1");
		ClientIpResolver resolver = new ClientIpResolver(properties);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("203.0.113.10");
		when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

		assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
	}

	@Test
	void usesLeftmostForwardedIpWhenPeerIsTrustedProxy() {
		AppProperties properties = new AppProperties();
		properties.getClientIp().setTrustForwardedHeaders(true);
		properties.getClientIp().setTrustedProxies("10.0.0.1");
		ClientIpResolver resolver = new ClientIpResolver(properties);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");
		when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.20, 10.0.0.1");

		assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
	}
}
