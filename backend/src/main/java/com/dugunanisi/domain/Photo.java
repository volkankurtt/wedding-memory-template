package com.dugunanisi.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "photos")
public class Photo {

	@Id
	private UUID id;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "storage_path", nullable = false, length = 255)
	private String storagePath;

	@Column(name = "client_upload_id", length = 36, unique = true)
	private String clientUploadId;

	@Column(name = "public_path", length = 512)
	private String publicPath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PhotoStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Photo() {
	}

	public Photo(UUID id, String originalFileName, String contentType, long sizeBytes, String storagePath) {
		this(id, originalFileName, contentType, sizeBytes, storagePath, null);
	}

	public Photo(
			UUID id,
			String originalFileName,
			String contentType,
			long sizeBytes,
			String storagePath,
			String clientUploadId) {
		this.id = id;
		this.originalFileName = originalFileName;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.storagePath = storagePath;
		this.clientUploadId = clientUploadId;
		this.status = PhotoStatus.PENDING;
		this.createdAt = Instant.now();
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (status == null) {
			status = PhotoStatus.PENDING;
		}
	}

	public void markReady(String publicPath) {
		this.publicPath = publicPath;
		this.status = PhotoStatus.READY;
	}

	public void markFailed() {
		this.status = PhotoStatus.FAILED;
	}

	public UUID getId() {
		return id;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public String getClientUploadId() {
		return clientUploadId;
	}

	public String getPublicPath() {
		return publicPath;
	}

	public PhotoStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
