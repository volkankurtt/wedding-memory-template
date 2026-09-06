package com.dugunanisi.ratelimit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dugunanisi.config.AppProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitFilter extends OncePerRequestFilter {

	private final SlidingWindowRateLimiter limiter;
	private final ClientIpResolver clientIp;
	private final AppProperties.RateLimit settings;

	public RateLimitFilter(
			SlidingWindowRateLimiter limiter,
			ClientIpResolver clientIp,
			AppProperties properties) {
		this.limiter = limiter;
		this.clientIp = clientIp;
		this.settings = properties.getRateLimit();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		LimitRule rule = ruleFor(request);
		if (rule == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String ip = clientIp.resolve(request);
		String key = rule.keyPrefix() + ":" + ip;
		if (!limiter.tryAcquire(key, rule.limit(), settings.getWindow())) {
			response.setStatus(429);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"error\":\"" + rule.message() + "\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private LimitRule ruleFor(HttpServletRequest request) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return null;
		}
		String path = request.getRequestURI();
		if ("/api/photos".equals(path)) {
			return new LimitRule("photos", settings.getPhotoUploadsPerWindow(),
					"Çok fazla fotoğraf yükleme denemesi. Lütfen biraz sonra tekrar deneyin.");
		}
		if ("/api/memories".equals(path)) {
			return new LimitRule("memories", settings.getMemoryPostsPerWindow(),
					"Çok fazla anı gönderildi. Lütfen biraz sonra tekrar deneyin.");
		}
		return null;
	}

	private record LimitRule(String keyPrefix, int limit, String message) {
	}
}
