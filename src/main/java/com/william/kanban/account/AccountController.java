package com.william.kanban.account;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
class AccountController {

	private final AccountService service;

	AccountController(AccountService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AccountResponse create(

			@Valid
			@RequestBody
			CreateAccountRequest request

	) {
		return toResponse(
				service.create(request.email(), request.displayName(), request.password()));
	}

	@GetMapping("/me")
	AccountResponse me(

			@AuthenticationPrincipal
			UUID accountId

	) {
		return toResponse(service.findById(accountId));
	}

	private AccountResponse toResponse(Account account) {
		return new AccountResponse(account.getId(), account.getEmail(), account.getDisplayName());
	}

}
