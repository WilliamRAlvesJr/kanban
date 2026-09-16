package com.william.kanban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "accounts")
public class Account {

	@Id
	private UUID id;

	private String email;

	private String displayName;

	private String passwordHash;

	@Column(insertable = false, updatable = false)
	@Generated(event = EventType.INSERT)
	private OffsetDateTime createdAt;

	protected Account() {
	}

	public Account(String email, String displayName, String passwordHash) {
		this.id = UUID.randomUUID();
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

}
