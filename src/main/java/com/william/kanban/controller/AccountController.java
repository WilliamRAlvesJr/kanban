package com.william.kanban.controller;

import com.william.kanban.dto.account.AccountResponse;
import com.william.kanban.dto.account.CreateAccountRequest;
import com.william.kanban.service.AccountService;
import com.william.kanban.shared.LinksModel;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	private static final String ME = "/accounts/me";

	private final AccountService service;

	AccountController(AccountService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<LinksModel> create(

			@Valid
			@RequestBody
			CreateAccountRequest request

	) {
		service.create(request);
		return ResponseEntity.created(URI.create(ME))
			.body(new LinksModel(Link.of(ME), Link.of("/auth/login", "login")));
	}

	@GetMapping("/me")
	EntityModel<AccountResponse> me(

			@AuthenticationPrincipal
			UUID accountId

	) {
		return EntityModel.of(
			service.findById(accountId),
			Link.of(ME),
			Link.of("/projects", "projects")
		);
	}

}
