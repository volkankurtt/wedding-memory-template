package com.dugunanisi.api;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	private final DataSource dataSource;

	public HealthController(ObjectProvider<DataSource> dataSource) {
		this.dataSource = dataSource.getIfAvailable();
	}

	@GetMapping("/health")
	public Map<String, String> health() {
		long started = System.nanoTime();
		if (dataSource != null) {
			try (Connection connection = dataSource.getConnection()) {
				if (!connection.isValid(3)) {
					throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Sunucuya ulaşılamadı. Lütfen tekrar deneyin.");
				}
			}
			catch (ApiException exception) {
				throw exception;
			}
			catch (Exception exception) {
				log.warn("health db ping failed ms={}: {}", elapsedMs(started), exception.toString());
				throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Sunucuya ulaşılamadı. Lütfen tekrar deneyin.");
			}
		}
		log.info("health ok dbPingMs={}", elapsedMs(started));
		return Map.of("status", "ok");
	}

	private static long elapsedMs(long startedNanos) {
		return (System.nanoTime() - startedNanos) / 1_000_000L;
	}
}
