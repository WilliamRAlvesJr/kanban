package com.william.kanban.lane;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "lanes")
class Lane {

	@Id
	private UUID id;

	private UUID boardId;

	private String name;

	private Integer position;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	private OffsetDateTime updatedAt;

	private OffsetDateTime archivedAt;

	protected Lane() {
	}

	Lane(UUID boardId, String name, int position) {
		this.id = UUID.randomUUID();
		this.boardId = boardId;
		this.name = name;
		this.position = position;
	}

	UUID getId() {
		return id;
	}

	UUID getBoardId() {
		return boardId;
	}

	String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	Integer getPosition() {
		return position;
	}

	void setPosition(Integer position) {
		this.position = position;
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
