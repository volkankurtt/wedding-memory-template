package com.dugunanisi.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	HttpClient supabaseHttpClient() {
		return HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	@Bean
	RestClient.Builder restClientBuilder(HttpClient supabaseHttpClient) {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(supabaseHttpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(60));
		return RestClient.builder().requestFactory(requestFactory);
	}
}
