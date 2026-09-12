package com.william.kanban.shared;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.function.Predicate;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Configuration
class OpenApiResponsesConfig {

	@Bean
	OperationCustomizer errorResponsesFromSignature() {
		return (operation, handlerMethod) -> {
			MethodParameter[] parameters = handlerMethod.getMethodParameters();
			if (any(parameters, p -> p.hasParameterAnnotation(Valid.class)
					|| p.hasParameterAnnotation(RequestParam.class))) {
				addProblem(operation, "400", "Dados de entrada inválidos");
			}
			if (any(parameters, p -> p.hasParameterAnnotation(AuthenticationPrincipal.class)
					|| Authentication.class.isAssignableFrom(p.getParameterType()))) {
				addProblem(operation, "401", "Token ausente, desconhecido ou expirado");
			}
			if (any(parameters, p -> p.hasParameterAnnotation(PathVariable.class)
					&& "projectId".equals(p.getParameter().getName()))) {
				addProblem(operation, "403", "Permissão de projeto ausente");
			}
			if (any(parameters, p -> p.hasParameterAnnotation(PathVariable.class))) {
				addProblem(operation, "404", "Recurso não encontrado");
			}
			return operation;
		};
	}

	@Bean
	OpenApiCustomizer errorResponsesOutsideSignature() {
		return openApi -> {
			ModelConverters.getInstance().read(ProblemDetail.class).forEach(openApi.getComponents()::addSchemas);
			addProblem(openApi.getPaths().get("/accounts").getPost(), "409", "Email já cadastrado");
			addProblem(openApi.getPaths().get("/auth/login").getPost(), "401", "Email ou senha inválidos");
			addProblem(openApi.getPaths().get("/boards/{boardId}/lanes/order").getPut(), "409",
					"Lista diferente das lanes ativas do quadro");
		};
	}

	private static boolean any(MethodParameter[] parameters, Predicate<MethodParameter> test) {
		return Arrays.stream(parameters).anyMatch(test);
	}

	private static void addProblem(Operation operation, String code, String description) {
		operation.getResponses().addApiResponse(code, new ApiResponse()
				.description(description)
				.content(new Content().addMediaType("application/problem+json",
						new MediaType().schema(new Schema<>().$ref("ProblemDetail")))));
	}

}
