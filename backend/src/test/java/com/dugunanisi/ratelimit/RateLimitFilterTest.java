package com.dugunanisi.ratelimit;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.dugunanisi.config.AppProperties;

class RateLimitFilterTest {

	@Test
	void returns429WhenMemoryPostsExceedLimit() throws Exception {
		AppProperties properties = new AppProperties();
		properties.getRateLimit().setMemoryPostsPerWindow(1);
		properties.getRateLimit().setWindow(Duration.ofMinutes(10));
		RateLimitFilter filter = new RateLimitFilter(
				new SlidingWindowRateLimiter(),
				new ClientIpResolver(properties),
				properties);

		MockHttpServletRequest first = postRequest("/api/memories", "10.1.1.1");
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(first, firstResponse, new MockFilterChain());
		org.assertj.core.api.Assertions.assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletRequest second = postRequest("/api/memories", "10.1.1.1");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(second, secondResponse, new MockFilterChain());

		org.assertj.core.api.Assertions.assertThat(secondResponse.getStatus()).isEqualTo(429);
		org.assertj.core.api.Assertions.assertThat(secondResponse.getContentAsString())
				.contains("Çok fazla anı gönderildi");
	}

	@Test
	void doesNotLimitGetRequests() throws Exception {
		AppProperties properties = new AppProperties();
		properties.getRateLimit().setPhotoUploadsPerWindow(1);
		RateLimitFilter filter = new RateLimitFilter(
				new SlidingWindowRateLimiter(),
				new ClientIpResolver(properties),
				properties);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/photos");
		request.setRemoteAddr("10.1.1.1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(200);

		MockHttpServletResponse second = new MockHttpServletResponse();
		filter.doFilter(request, second, new MockFilterChain());
		org.assertj.core.api.Assertions.assertThat(second.getStatus()).isEqualTo(200);
	}

	private static MockHttpServletRequest postRequest(String path, String ip) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
		request.setRemoteAddr(ip);
		request.setContentType(MediaType.APPLICATION_JSON_VALUE);
		return request;
	}
}
