package com.dugunanisi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.dugunanisi.image.FakeImageConverter;
import com.dugunanisi.image.ImageConverter;
import com.dugunanisi.storage.InMemoryObjectStorage;
import com.dugunanisi.storage.ObjectStorage;

@TestConfiguration
public class PhotoTestConfiguration {

	@Bean
	@Primary
	ObjectStorage objectStorage() {
		return new InMemoryObjectStorage();
	}

	@Bean
	@Primary
	ImageConverter imageConverter() {
		return new FakeImageConverter();
	}
}
