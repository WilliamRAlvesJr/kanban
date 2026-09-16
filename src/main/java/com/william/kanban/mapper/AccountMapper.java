package com.william.kanban.mapper;

import com.william.kanban.dto.account.AccountResponse;
import com.william.kanban.dto.account.AccountSummary;
import com.william.kanban.dto.account.CreateAccountRequest;
import com.william.kanban.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

	public Account toEntity(CreateAccountRequest request, String passwordHash) {
		return new Account(request.email(), request.displayName(), passwordHash);
	}

	public AccountResponse toResponse(Account account) {
		return new AccountResponse(
			account.getId(),
			account.getEmail(),
			account.getDisplayName()
		);
	}

	public AccountSummary toSummary(Account account) {
		return new AccountSummary(
			account.getId(),
			account.getEmail(),
			account.getDisplayName()
		);
	}

}
