package com.dugunanisi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.dugunanisi.api.ApiException;
import com.dugunanisi.api.dto.CreateMemoryRequest;
import com.dugunanisi.domain.Memory;
import com.dugunanisi.domain.MemoryRepository;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

	@Mock
	private MemoryRepository repository;

	private MemoryService service;

	@BeforeEach
	void setUp() {
		service = new MemoryService(repository);
	}

	@Test
	void blankNameBecomesAnonymous() {
		when(repository.save(any(Memory.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

		var response = service.create(new CreateMemoryRequest("   ", "  Mutluluklar  "));

		assertThat(response.name()).isEqualTo("Anonim");
		assertThat(response.message()).isEqualTo("Mutluluklar");

		ArgumentCaptor<Memory> captor = ArgumentCaptor.forClass(Memory.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("Anonim");
		assertThat(captor.getValue().getMessage()).isEqualTo("Mutluluklar");
	}

	@Test
	void emptyMessageIsRejected() {
		assertThatThrownBy(() -> service.create(new CreateMemoryRequest("Ayşe", "   ")))
				.isInstanceOf(ApiException.class)
				.hasMessage("Lütfen bir anı veya mesaj yazın.")
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void messageLongerThan600IsRejected() {
		String tooLong = "a".repeat(601);

		assertThatThrownBy(() -> service.create(new CreateMemoryRequest("Ayşe", tooLong)))
				.isInstanceOf(ApiException.class)
				.hasMessage("Anı en fazla 600 karakter olabilir.");
	}

	@Test
	void nameLongerThan80IsRejected() {
		String tooLong = "n".repeat(81);

		assertThatThrownBy(() -> service.create(new CreateMemoryRequest(tooLong, "Güzel bir gece")))
				.isInstanceOf(ApiException.class)
				.hasMessage("Ad en fazla 80 karakter olabilir.");
	}

	@Test
	void htmlIsStoredAsPlainText() {
		when(repository.save(any(Memory.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

		var response = service.create(new CreateMemoryRequest("Ali", "<script>alert(1)</script>"));

		assertThat(response.message()).isEqualTo("<script>alert(1)</script>");
	}

	@Test
	void listUsesCreatedAtDescending() {
		Memory newer = persisted(new Memory("B", "yeni"));
		Memory older = persisted(new Memory("A", "eski"));
		ReflectionTestUtils.setField(newer, "createdAt", Instant.parse("2026-09-07T10:00:00Z"));
		ReflectionTestUtils.setField(older, "createdAt", Instant.parse("2026-09-06T10:00:00Z"));
		when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newer, older));

		var list = service.list();

		assertThat(list).extracting(item -> item.message()).containsExactly("yeni", "eski");
	}

	private static Memory persisted(Memory memory) {
		ReflectionTestUtils.setField(memory, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(memory, "createdAt", Instant.parse("2026-09-07T12:00:00Z"));
		return memory;
	}
}
