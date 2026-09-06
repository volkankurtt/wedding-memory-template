package com.dugunanisi.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "memories")
public class Memory {

	@Id
	private UUID id;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(nullable = false, length = 600)
	private String message;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Memory() {
	}

	public Memory(String name, String message) {
		this.name = name;
		this.message = message;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getMessage() {
		return message;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
