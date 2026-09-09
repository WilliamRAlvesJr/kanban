package com.william.kanban.account;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Conta criada",
					content = @Content(schema = @Schema(implementation = AccountResponse.class))),
			@ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "409", description = "Email já cadastrado",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
	AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
		return toResponse(
				service.create(request.email(), request.displayName(), request.password()));
	}

	@GetMapping("/me")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Conta encontrada",
					content = @Content(schema = @Schema(implementation = AccountResponse.class))),
			@ApiResponse(responseCode = "401", description = "Token ausente, desconhecido ou expirado",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
	AccountResponse me(@AuthenticationPrincipal UUID accountId) {
		return toResponse(service.findById(accountId));
	}

	private AccountResponse toResponse(Account account) {
		return new AccountResponse(account.getId(), account.getEmail(), account.getDisplayName());
	}

}
