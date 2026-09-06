package com.dugunanisi.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.dugunanisi.ratelimit.ClientIpResolver;
import com.dugunanisi.ratelimit.RateLimitFilter;
import com.dugunanisi.ratelimit.SlidingWindowRateLimiter;

@Configuration
public class RateLimitConfiguration {

	@Bean
	SlidingWindowRateLimiter slidingWindowRateLimiter() {
		return new SlidingWindowRateLimiter();
	}

	@Bean
	ClientIpResolver clientIpResolver(AppProperties properties) {
		return new ClientIpResolver(properties);
	}

	@Bean
	FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
			SlidingWindowRateLimiter limiter,
			ClientIpResolver clientIp,
			AppProperties properties) {
		FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(
				new RateLimitFilter(limiter, clientIp, properties));
		registration.setName("rateLimitFilter");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
		registration.addUrlPatterns("/api/*");
		return registration;
	}
}
