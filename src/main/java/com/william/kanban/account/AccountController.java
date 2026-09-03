package com.william.kanban.account;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
class AccountController {

	private final AccountService service;

	AccountController(AccountService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
		Account account = service.create(request.email(), request.displayName(), request.password());
		return ResponseEntity.created(URI.create("/accounts/" + account.getId()))
				.body(new AccountResponse(account.getId(), account.getEmail(), account.getDisplayName()));
	}

}
