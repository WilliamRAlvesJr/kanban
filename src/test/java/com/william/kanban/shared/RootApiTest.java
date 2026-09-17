package com.william.kanban.shared;

import static com.william.kanban.support.ApiClient.link;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.william.kanban.TestcontainersConfiguration;
import com.william.kanban.auth.LoginRequest;
import com.william.kanban.dto.account.CreateAccountRequest;
import com.william.kanban.support.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RootApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	ApiClient api;

	@BeforeEach
	void setUp() {
		api = new ApiClient(mockMvc);
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void linksAnonymousEntryPoint() {
		expectAnonymousLinks(api.get("/").perform());
	}

	@Test
	void linksAuthenticatedEntryPoint() {
		api.get("/")
			.withToken(tokenOfAna())
			.perform()
			.expectStatus(OK)
			.expectJson("$", aMapWithSize(1))
			.expectLinks(
				link("self", "/"),
				link("me", "/accounts/me"),
				link("projects", "/projects"),
				link("create-project", "/projects"),
				link("logout", "/auth/logout")
			);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"Bearer nunca-emitido",
		"Basic YWJj"
	})
	void linksEntryPointAsAnonymousForUnacceptedHeader(String header) {
		expectAnonymousLinks(api.get("/")
			.withHeader(AUTHORIZATION, header)
			.perform()
		);
	}

	@Test
	void linksEntryPointAsAnonymousForExpiredToken() {
		var token = tokenOfAna();
		jdbcTemplate.update(
			"update auth_tokens set expires_at = now() - interval '1 minute'"
		);

		expectAnonymousLinks(api.get("/")
			.withToken(token)
			.perform()
		);
	}

	@Test
	void documentsEntryPoint() {
		api.get("/v3/api-docs")
			.perform()
			.expectStatus(OK)
			.expectPresent("$.paths['/'].get.responses.200")
			.expectAbsent("$.paths['/'].get.responses.401")
			.expectAbsent("$.paths['/'].get.parameters")
			.expectAbsent("$.components.schemas.SecurityContext");
	}

	private void expectAnonymousLinks(ApiClient.Response response) {
		response
			.expectStatus(OK)
			.expectJson("$", aMapWithSize(1))
			.expectLinks(
				link("self", "/"),
				link("login", "/auth/login"),
				link("create-account", "/accounts")
			);
	}

	private String tokenOfAna() {
		api.post("/accounts")
			.withBody(new CreateAccountRequest("ana@exemplo.com", "Ana", "segredo"))
			.perform()
			.expectStatus(CREATED);
		return api.post("/auth/login")
			.withBody(new LoginRequest("ana@exemplo.com", "segredo"))
			.perform()
			.expectStatus(CREATED)
			.json("$.token");
	}

}
