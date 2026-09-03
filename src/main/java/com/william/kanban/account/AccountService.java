package com.william.kanban.account;

import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class AccountService {

	private final AccountRepository repository;

	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	AccountService(AccountRepository repository) {
		this.repository = repository;
	}

	Account create(String email, String displayName, String password) {
		return repository.save(
				new Account(email.toLowerCase(Locale.ROOT), displayName, encoder.encode(password)));
	}

	Account findById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
	}

}
