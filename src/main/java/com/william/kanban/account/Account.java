package com.william.kanban.account;

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
class Account {

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

	Account(String email, String displayName, String passwordHash) {
		this.id = UUID.randomUUID();
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
	}

	UUID getId() {
		return id;
	}

	String getEmail() {
		return email;
	}

	String getDisplayName() {
		return displayName;
	}

}
