package com.william.kanban.service;

import com.william.kanban.dto.auth.LoginRequest;
import com.william.kanban.dto.auth.LoginResponse;
import com.william.kanban.entity.AuthToken;
import com.william.kanban.exception.InvalidCredentialsException;
import com.william.kanban.repository.AuthTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	public static final String BEARER_PREFIX = "Bearer ";

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	private final AuthTokenRepository repository;

	private final AccountService accounts;

	private final Duration tokenTtl;

	AuthService(

			AuthTokenRepository repository,

			AccountService accounts,

			@Value("${kanban.auth.token-ttl}")
			Duration tokenTtl

	) {
		this.repository = repository;
		this.accounts = accounts;
		this.tokenTtl = tokenTtl;
	}

	public LoginResponse login(LoginRequest request) {
		var accountId = accounts.authenticate(request.email(), request.password())
			.orElseThrow(InvalidCredentialsException::new);
		var bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		var value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		var expiresAt = OffsetDateTime.now(ZoneId.systemDefault()).plus(tokenTtl);
		repository.save(new AuthToken(accountId, hash(value), expiresAt));
		return new LoginResponse(value, "Bearer", expiresAt);
	}

	@Transactional
	public void logout(String token) {
		repository.deleteByTokenHash(hash(token));
	}

	public Optional<UUID> resolve(String token) {
		var now = OffsetDateTime.now(ZoneId.systemDefault());
		return repository.findByTokenHash(hash(token))
			.filter(found -> found.getExpiresAt().isAfter(now))
			.map(AuthToken::getAccountId);
	}

	public static String stripBearer(String header) {
		return header.substring(BEARER_PREFIX.length());
	}

	private static String hash(String token) {
		try {
			var sha256 = MessageDigest.getInstance("SHA-256");
			var digest = sha256.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
