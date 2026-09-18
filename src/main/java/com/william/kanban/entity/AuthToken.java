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
@Table(name = "auth_tokens")
public class AuthToken {

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

	public AuthToken(UUID accountId, String tokenHash, OffsetDateTime expiresAt) {
		this.id = UUID.randomUUID();
		this.accountId = accountId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

}
