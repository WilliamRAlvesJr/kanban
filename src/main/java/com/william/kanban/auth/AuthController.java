package com.william.kanban.auth;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController {

	private final AuthService service;

	AuthController(AuthService service) {
		this.service = service;
	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.CREATED)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Token emitido",
					content = @Content(schema = @Schema(implementation = LoginResponse.class))),
			@ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "401", description = "Email ou senha inválidos",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
	LoginResponse login(@Valid @RequestBody LoginRequest request) {
		IssuedToken token = service.login(request.email(), request.password());
		return new LoginResponse(token.value(), "Bearer", token.expiresAt());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Token revogado"),
			@ApiResponse(responseCode = "401", description = "Token ausente, desconhecido ou expirado",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
	void logout(Authentication authentication) {
		service.logout((String) authentication.getCredentials());
	}

}
