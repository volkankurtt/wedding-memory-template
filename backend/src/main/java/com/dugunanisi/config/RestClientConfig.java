package com.dugunanisi.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	RestClient.Builder restClientBuilder() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(15));
		requestFactory.setReadTimeout(Duration.ofSeconds(60));
		return RestClient.builder().requestFactory(requestFactory);
	}
}
