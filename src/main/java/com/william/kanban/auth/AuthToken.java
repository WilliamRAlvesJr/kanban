package com.william.kanban.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "auth_tokens")
class AuthToken {

	@Id
	private UUID id;

	private UUID accountId;

	private String tokenHash;

	@Column(insertable = false, updatable = false)
	@Generated(event = EventType.INSERT)
	private OffsetDateTime createdAt;

	private OffsetDateTime expiresAt;

	protected AuthToken() {
	}

	AuthToken(UUID accountId, String tokenHash, OffsetDateTime expiresAt) {
		this.id = UUID.randomUUID();
		this.accountId = accountId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	UUID getAccountId() {
		return accountId;
	}

	OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

}
