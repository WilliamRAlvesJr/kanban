package com.william.kanban.account;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Conta criada",
					content = @Content(schema = @Schema(implementation = AccountResponse.class))),
			@ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "409", description = "Email já cadastrado",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
	ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
		Account account = service.create(request.email(), request.displayName(), request.password());
		return ResponseEntity.created(URI.create("/accounts/" + account.getId()))
				.body(toResponse(account));
	}

	@GetMapping("/{id}")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Conta encontrada",
					content = @Content(schema = @Schema(implementation = AccountResponse.class))),
			@ApiResponse(responseCode = "400", description = "Id fora do formato uuid",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "404", description = "Conta não encontrada",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
	AccountResponse findById(@PathVariable UUID id) {
		return toResponse(service.findById(id));
	}

	private AccountResponse toResponse(Account account) {
		return new AccountResponse(account.getId(), account.getEmail(), account.getDisplayName());
	}

}
