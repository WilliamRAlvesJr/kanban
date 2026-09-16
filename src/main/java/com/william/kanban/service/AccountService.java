package com.william.kanban.service;

import static java.util.stream.Collectors.toMap;

import com.william.kanban.dto.account.AccountResponse;
import com.william.kanban.dto.account.AccountSummary;
import com.william.kanban.dto.account.CreateAccountRequest;
import com.william.kanban.entity.Account;
import com.william.kanban.exception.AccountNotFoundException;
import com.william.kanban.mapper.AccountMapper;
import com.william.kanban.repository.AccountRepository;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

	private final AccountRepository repository;

	private final PasswordEncoder encoder;

	private final AccountMapper mapper;

	/**
	 * Hash de conta nenhuma: comparar contra ele iguala o custo do login com email não
	 * cadastrado ao do email conhecido.
	 */
	private final String absentAccountHash;

	AccountService(
		AccountRepository repository,
		PasswordEncoder encoder,
		AccountMapper mapper
	) {
		this.repository = repository;
		this.encoder = encoder;
		this.mapper = mapper;
		this.absentAccountHash = encoder.encode(UUID.randomUUID().toString());
	}

	public void create(CreateAccountRequest request) {
		var email = request.email().toLowerCase(Locale.ROOT);
		var passwordHash = encoder.encode(request.password());
		repository.save(mapper.toEntity(request.withEmail(email), passwordHash));
	}

	public Optional<UUID> authenticate(String email, String password) {
		var account = repository.findByEmail(email.toLowerCase(Locale.ROOT));
		if (account.isEmpty()) {
			encoder.matches(password, absentAccountHash);
			return Optional.empty();
		}
		if (!encoder.matches(password, account.get().getPasswordHash())) {
			return Optional.empty();
		}
		return Optional.of(account.get().getId());
	}

	public Optional<UUID> findIdByEmail(String email) {
		return repository.findByEmail(email.toLowerCase(Locale.ROOT))
			.map(Account::getId);
	}

	public Map<UUID, AccountSummary> summariesOf(Collection<UUID> ids) {
		return repository.findAllById(ids)
			.stream()
			.collect(toMap(Account::getId, mapper::toSummary));
	}

	public AccountResponse findById(UUID id) {
		return repository.findById(id)
			.map(mapper::toResponse)
			.orElseThrow(() -> new AccountNotFoundException(id));
	}

}
