package com.william.kanban.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class BearerAuthenticationFilter extends OncePerRequestFilter {

	private final AuthService service;

	BearerAuthenticationFilter(AuthService service) {
		this.service = service;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(AuthService.BEARER_PREFIX)) {
			String token = AuthService.stripBearer(header);
			service.resolve(token)
					.map(accountId -> authenticated(accountId, token))
					.ifPresent(SecurityContextHolder.getContext()::setAuthentication);
		}
		chain.doFilter(request, response);
	}

	/** O token vai nas credentials: o logout revoga a linha da própria requisição, não toda a conta. */
	private static Authentication authenticated(UUID accountId, String token) {
		return new UsernamePasswordAuthenticationToken(accountId, token, List.of());
	}

}
