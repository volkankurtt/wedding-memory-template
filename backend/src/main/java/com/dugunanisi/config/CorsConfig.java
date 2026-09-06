package com.dugunanisi.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

	@Bean
	CorsFilter corsFilter(AppProperties properties) {
		List<String> origins = properties.getCors().allowedOriginList();
		if (origins.isEmpty()) {
			origins = List.of("http://localhost:5173");
		}

		CorsConfiguration guestWrite = new CorsConfiguration();
		guestWrite.setAllowedOrigins(origins);
		guestWrite.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
		guestWrite.setAllowedHeaders(List.of("Content-Type", "Accept"));
		guestWrite.setAllowCredentials(false);
		guestWrite.setMaxAge(3600L);

		CorsConfiguration health = new CorsConfiguration();
		health.setAllowedOrigins(origins);
		health.setAllowedMethods(List.of("GET", "OPTIONS"));
		health.setAllowedHeaders(List.of("Accept"));
		health.setAllowCredentials(false);
		health.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/photos", guestWrite);
		source.registerCorsConfiguration("/api/memories", guestWrite);
		source.registerCorsConfiguration("/api/health", health);
		return new CorsFilter(source);
	}
}
