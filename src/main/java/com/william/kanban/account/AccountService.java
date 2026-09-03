package com.william.kanban.account;

import org.springframework.stereotype.Service;

@Service
class AccountService {

	private final AccountRepository repository;

	AccountService(AccountRepository repository) {
		this.repository = repository;
	}

	Account create(String email, String displayName, String password) {
		return repository.save(new Account(email, displayName, password));
	}

}
