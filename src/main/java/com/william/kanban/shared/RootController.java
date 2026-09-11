package com.william.kanban.shared;

import java.util.List;
import java.util.UUID;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RootController {

	/**
	 * Lê a conta pelo {@code SecurityContextHolder}, sem parâmetro: {@code @AuthenticationPrincipal} faz o
	 * OpenApiResponsesConfig documentar 401 na rota aberta, e {@code @CurrentSecurityContext} vira parâmetro de
	 * query no springdoc.
	 */
	@GetMapping("/")
	RepresentationModel<?> root() {
		if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UUID) {
			return new RepresentationModel<>(List.of(
					Link.of("/"),
					Link.of("/accounts/me", "me"),
					Link.of("/projects", "projects"),
					Link.of("/projects", "create-project"),
					Link.of("/auth/logout", "logout")));
		}
		return new RepresentationModel<>(List.of(
				Link.of("/"),
				Link.of("/auth/login", "login"),
				Link.of("/accounts", "create-account")));
	}

}
