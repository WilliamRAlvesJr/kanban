package com.william.kanban.auth;

import com.william.kanban.account.AccountService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {

	static final String BEARER_PREFIX = "Bearer ";

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	private final AuthTokenRepository repository;

	private final AccountService accounts;

	private final Duration tokenTtl;

	AuthService(AuthTokenRepository repository, AccountService accounts,
			@Value("${kanban.auth.token-ttl}") Duration tokenTtl) {
		this.repository = repository;
		this.accounts = accounts;
		this.tokenTtl = tokenTtl;
	}

	IssuedToken login(String email, String password) {
		UUID accountId =
				accounts.authenticate(email, password).orElseThrow(InvalidCredentialsException::new);
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		OffsetDateTime expiresAt = OffsetDateTime.now().plus(tokenTtl);
		repository.save(new AuthToken(accountId, hash(value), expiresAt));
		return new IssuedToken(value, expiresAt);
	}

	@Transactional
	void logout(String token) {
		repository.deleteByTokenHash(hash(token));
	}

	Optional<UUID> resolve(String token) {
		return repository.findByTokenHash(hash(token))
				.filter(found -> found.getExpiresAt().isAfter(OffsetDateTime.now()))
				.map(AuthToken::getAccountId);
	}

	static String stripBearer(String header) {
		return header.substring(BEARER_PREFIX.length());
	}

	private static String hash(String token) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
