package com.william.kanban.account;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

	/** Hash de conta nenhuma: comparar contra ele iguala o custo do login com email não cadastrado ao do email conhecido. */
	private static final String ABSENT_ACCOUNT_HASH =
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final AccountRepository repository;

	private final PasswordEncoder encoder;

	AccountService(AccountRepository repository, PasswordEncoder encoder) {
		this.repository = repository;
		this.encoder = encoder;
	}

	Account create(String email, String displayName, String password) {
		return repository.save(
				new Account(email.toLowerCase(Locale.ROOT), displayName, encoder.encode(password)));
	}

	public Optional<UUID> authenticate(String email, String password) {
		Optional<Account> account = repository.findByEmail(email.toLowerCase(Locale.ROOT));
		if (account.isEmpty()) {
			encoder.matches(password, ABSENT_ACCOUNT_HASH);
			return Optional.empty();
		}
		if (!encoder.matches(password, account.get().getPasswordHash())) {
			return Optional.empty();
		}
		return Optional.of(account.get().getId());
	}

	Account findById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
	}

}
