package com.william.kanban.project;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "project_members")
class ProjectMember {

	@Id
	private UUID id;

	private UUID projectId;

	private UUID accountId;

	@Column(insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@ElementCollection
	@CollectionTable(name = "project_member_permissions", joinColumns = @JoinColumn(name = "project_member_id"))
	@Column(name = "permission")
	@Enumerated(EnumType.STRING)
	private Set<ProjectPermission> permissions = new HashSet<>();

	protected ProjectMember() {
	}

	UUID getId() {
		return id;
	}

	UUID getProjectId() {
		return projectId;
	}

	UUID getAccountId() {
		return accountId;
	}

	OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	Set<ProjectPermission> getPermissions() {
		return permissions;
	}

}
