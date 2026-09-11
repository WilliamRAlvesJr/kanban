package com.william.kanban.auth;

import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
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
	EntityModel<LoginResponse> login(

			@Valid
			@RequestBody
			LoginRequest request

	) {
		IssuedToken token = service.login(request.email(), request.password());
		return EntityModel.of(new LoginResponse(token.value(), "Bearer", token.expiresAt()),
				Link.of("/accounts/me", "me"), Link.of("/auth/logout", "logout"));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(Authentication authentication) {
		service.logout((String) authentication.getCredentials());
	}

}
