package com.william.kanban.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper mapper;

	ProblemDetailAuthenticationEntryPoint(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception
	) throws IOException {
		var problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.UNAUTHORIZED,
			"Token ausente, desconhecido ou expirado."
		);
		problem.setInstance(URI.create(request.getRequestURI()));
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		mapper.writeValue(response.getOutputStream(), problem);
	}

}
