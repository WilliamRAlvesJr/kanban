package com.william.kanban.shared;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.william.kanban.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RootApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void linksAnonymousEntryPoint() throws Exception {
		expectAnonymousLinks(mockMvc.perform(get("/")));
	}

	@Test
	void linksAuthenticatedEntryPoint() throws Exception {
		mockMvc.perform(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOfAna()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$._links", aMapWithSize(5)))
				.andExpect(jsonPath("$._links.self.href").value("/"))
				.andExpect(jsonPath("$._links.me.href").value("/accounts/me"))
				.andExpect(jsonPath("$._links.projects.href").value("/projects"))
				.andExpect(jsonPath("$._links['create-project'].href").value("/projects"))
				.andExpect(jsonPath("$._links.logout.href").value("/auth/logout"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"Bearer nunca-emitido", "Basic YWJj"})
	void linksEntryPointAsAnonymousForUnacceptedHeader(String header) throws Exception {
		expectAnonymousLinks(mockMvc.perform(get("/").header(HttpHeaders.AUTHORIZATION, header)));
	}

	@Test
	void linksEntryPointAsAnonymousForExpiredToken() throws Exception {
		String token = tokenOfAna();
		jdbcTemplate.update("update auth_tokens set expires_at = now() - interval '1 minute'");

		expectAnonymousLinks(mockMvc.perform(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
	}

	@Test
	void documentsEntryPoint() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/'].get.responses.401").doesNotExist())
				.andExpect(jsonPath("$.paths['/'].get.parameters").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.SecurityContext").doesNotExist());
	}

	private void expectAnonymousLinks(ResultActions actions) throws Exception {
		actions.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$._links", aMapWithSize(3)))
				.andExpect(jsonPath("$._links.self.href").value("/"))
				.andExpect(jsonPath("$._links.login.href").value("/auth/login"))
				.andExpect(jsonPath("$._links['create-account'].href").value("/accounts"));
	}

	private String tokenOfAna() throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}
								"""))
				.andExpect(status().isCreated());
		String body = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana@exemplo.com", "password": "segredo"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$.token");
	}

}
