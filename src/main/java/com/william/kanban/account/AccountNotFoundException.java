package com.william.kanban.account;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

	AccountNotFoundException(UUID id) {
		super("Conta não encontrada: " + id);
	}

}
