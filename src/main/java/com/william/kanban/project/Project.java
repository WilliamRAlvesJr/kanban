package com.william.kanban.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "projects")
class Project {

	@Id
	private UUID id;

	private UUID ownerId;

	private String name;

	private String description;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	private OffsetDateTime updatedAt;

	private OffsetDateTime archivedAt;

	protected Project() {
	}

	Project(UUID ownerId, String name, String description) {
		this.id = UUID.randomUUID();
		this.ownerId = ownerId;
		this.name = name;
		this.description = description;
	}

	UUID getId() {
		return id;
	}

	UUID getOwnerId() {
		return ownerId;
	}

	String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	String getDescription() {
		return description;
	}

	void setDescription(String description) {
		this.description = description;
	}

	OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	OffsetDateTime getArchivedAt() {
		return archivedAt;
	}

	void setArchivedAt(OffsetDateTime archivedAt) {
		this.archivedAt = archivedAt;
	}

}
