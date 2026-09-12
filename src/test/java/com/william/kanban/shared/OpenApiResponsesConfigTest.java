package com.william.kanban.shared;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

class OpenApiResponsesConfigTest {

	private final OpenApiResponsesConfig config = new OpenApiResponsesConfig();

	@Test
	void documents400ForValidatedBody() {
		assertThat(responsesOf("withValidatedBody")).containsOnlyKeys("400");
	}

	@Test
	void documents400ForRequestParam() {
		assertThat(responsesOf("withRequestParam")).containsOnlyKeys("400");
	}

	@Test
	void documents401ForAuthenticationPrincipal() {
		assertThat(responsesOf("withAuthenticationPrincipal")).containsOnlyKeys("401");
	}

	@Test
	void documents401ForAuthenticationParameter() {
		assertThat(responsesOf("withAuthentication")).containsOnlyKeys("401");
	}

	@Test
	void documents404ForPathVariable() {
		assertThat(responsesOf("withPathVariable")).containsOnlyKeys("404");
	}

	@Test
	void documents403ForProjectIdPathVariable() {
		assertThat(responsesOf("withProjectIdPathVariable")).containsOnlyKeys("403", "404");
	}

	@Test
	void documentsNothingForPlainParameter() {
		assertThat(responsesOf("withPlainParameter")).isEmpty();
	}

	@Test
	void pointsErrorResponseToProblemDetailSchema() {
		ApiResponse response = responsesOf("withPathVariable").get("404");

		assertThat(response.getDescription()).isEqualTo("Recurso não encontrado");
		assertThat(response.getContent().get("application/problem+json").getSchema().get$ref())
				.isEqualTo("#/components/schemas/ProblemDetail");
	}

	@Test
	void registersProblemDetailSchema() {
		OpenAPI openApi = documentWithCustomizedRoutes();

		config.errorResponsesOutsideSignature().customise(openApi);

		assertThat(openApi.getComponents().getSchemas()).containsKey("ProblemDetail");
	}

	@Test
	void documents409ForAccountCreation() {
		OpenAPI openApi = documentWithCustomizedRoutes();

		config.errorResponsesOutsideSignature().customise(openApi);

		assertThat(openApi.getPaths().get("/accounts").getPost().getResponses()).containsOnlyKeys("409");
	}

	@Test
	void documents401ForLogin() {
		OpenAPI openApi = documentWithCustomizedRoutes();

		config.errorResponsesOutsideSignature().customise(openApi);

		assertThat(openApi.getPaths().get("/auth/login").getPost().getResponses()).containsOnlyKeys("401");
	}

	@Test
	void documents409ForLaneReorder() {
		OpenAPI openApi = documentWithCustomizedRoutes();

		config.errorResponsesOutsideSignature().customise(openApi);

		assertThat(openApi.getPaths().get("/boards/{boardId}/lanes/order").getPut().getResponses())
				.containsOnlyKeys("409");
	}

	private ApiResponses responsesOf(String methodName) {
		Method method = Arrays.stream(SampleController.class.getDeclaredMethods())
				.filter(m -> m.getName().equals(methodName))
				.findFirst()
				.orElseThrow();
		return config.errorResponsesFromSignature()
				.customize(emptyOperation(), new HandlerMethod(new Object(), method))
				.getResponses();
	}

	private OpenAPI documentWithCustomizedRoutes() {
		return new OpenAPI()
				.components(new Components())
				.paths(new Paths()
						.addPathItem("/accounts", new PathItem().post(emptyOperation()))
						.addPathItem("/auth/login", new PathItem().post(emptyOperation()))
						.addPathItem("/boards/{boardId}/lanes/order", new PathItem().put(emptyOperation())));
	}

	private Operation emptyOperation() {
		return new Operation().responses(new ApiResponses());
	}

	interface SampleController {

		void withValidatedBody(

				@Valid
				@RequestBody
				Object body

		);

		void withRequestParam(

				@RequestParam
				String query

		);

		void withAuthenticationPrincipal(

				@AuthenticationPrincipal
				UUID accountId

		);

		void withAuthentication(Authentication authentication);

		void withPathVariable(

				@PathVariable
				UUID id

		);

		void withProjectIdPathVariable(

				@PathVariable
				UUID projectId

		);

		void withPlainParameter(String value);

	}

}
