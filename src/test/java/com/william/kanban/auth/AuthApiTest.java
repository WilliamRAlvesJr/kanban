package com.william.kanban.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.william.kanban.TestcontainersConfiguration;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void issuesTokenForCorrectCredentials() throws Exception {
		createAna();

		mockMvc.perform(login("ana@exemplo.com", "segredo"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_at").isNotEmpty())
				.andExpect(jsonPath("$._links", aMapWithSize(2)))
				.andExpect(jsonPath("$._links.me.href").value("/accounts/me"))
				.andExpect(jsonPath("$._links.logout.href").value("/auth/logout"))
				.andExpect(header().doesNotExist(HttpHeaders.LOCATION));
	}

	@Test
	void issuesTokenForEmailInAnotherCase() throws Exception {
		createAna();

		mockMvc.perform(login("ANA@Exemplo.com", "segredo"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.token_type").value("Bearer"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"""
					{"password": "segredo"}""",
			"""
					{"email": "ana@exemplo.com"}""",
			"""
					{"email": "", "password": "segredo"}""",
			"""
					{"email": "ana@exemplo.com", "password": ""}"""
	})
	void rejectsPayloadWithRequiredFieldMissingOrBlank(String body) throws Exception {
		createAna();

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(countTokens()).isZero();
	}

	@Test
	void rejectsWrongPassword() throws Exception {
		createAna();

		mockMvc.perform(login("ana@exemplo.com", "errada"))
				.andExpect(status().isUnauthorized());

		assertThat(countTokens()).isZero();
	}

	@Test
	void answersUnknownEmailWithTheSameBodyAsWrongPassword() throws Exception {
		createAna();

		String wrongPassword = mockMvc.perform(login("ana@exemplo.com", "errada"))
				.andExpect(status().isUnauthorized())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknownEmail = mockMvc.perform(login("bruno@exemplo.com", "segredo"))
				.andExpect(status().isUnauthorized())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(unknownEmail).isEqualTo(wrongPassword);
		assertThat(countTokens()).isZero();
	}

	@Test
	void issuesIndependentTokenOnEachLogin() throws Exception {
		createAna();
		String first = tokenOf(login("ana@exemplo.com", "segredo"));
		String second = tokenOf(login("ana@exemplo.com", "segredo"));

		assertThat(second).isNotEqualTo(first);
		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
				.andExpect(status().isOk());
	}

	@Test
	void storesOnlyTheHashOfTheToken() throws Exception {
		createAna();
		String token = tokenOf(login("ana@exemplo.com", "segredo"));

		String tokenHash =
				jdbcTemplate.queryForObject("select token_hash from auth_tokens", String.class);
		assertThat(tokenHash).isNotEqualTo(token);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from auth_tokens where token_hash = ?", Integer.class, token))
				.isZero();
	}

	@Test
	void acceptsValidToken() throws Exception {
		String id = createAna();
		String token = tokenOf(login("ana@exemplo.com", "segredo"));

		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.email").value("ana@exemplo.com"));
	}

	@Test
	void returnsTheAccountOfTheTokenUsed() throws Exception {
		createAna();
		String brunoId = createAccount("bruno@exemplo.com", "Bruno", "outra");
		String brunoToken = tokenOf(login("bruno@exemplo.com", "outra"));

		mockMvc.perform(
						get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + brunoToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(brunoId))
				.andExpect(jsonPath("$.email").value("bruno@exemplo.com"));
	}

	@Test
	void acceptsTokenIssuedAnHourAgo() throws Exception {
		createAna();
		String token = tokenOf(login("ana@exemplo.com", "segredo"));
		jdbcTemplate.update("update auth_tokens set created_at = now() - interval '1 hour',"
				+ " expires_at = expires_at - interval '1 hour'");

		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsRequestWithoutAuthorizationHeader() throws Exception {
		mockMvc.perform(get("/accounts/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.instance").value("/accounts/me"));
	}

	@Test
	void rejectsAuthorizationHeaderInAnotherFormat() throws Exception {
		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Basic YWJj"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void rejectsValidTokenUnderAnotherScheme() throws Exception {
		createAna();
		String token = tokenOf(login("ana@exemplo.com", "segredo"));

		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Digest " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void rejectsUnknownToken() throws Exception {
		mockMvc.perform(
						get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer nunca-emitido"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void rejectsExpiredToken() throws Exception {
		createAna();
		String token = tokenOf(login("ana@exemplo.com", "segredo"));
		jdbcTemplate.update("update auth_tokens set expires_at = now() - interval '1 minute'");

		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesTheTokenUsed() throws Exception {
		createAna();
		String token = tokenOf(login("ana@exemplo.com", "segredo"));

		mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutKeepsTheOtherTokens() throws Exception {
		createAna();
		String first = tokenOf(login("ana@exemplo.com", "segredo"));
		String second = tokenOf(login("ana@exemplo.com", "segredo"));

		mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + first))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + second))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsLogoutWithoutToken() throws Exception {
		mockMvc.perform(post("/auth/logout"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void documentsAuthEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/auth/login'].post.responses.201").exists())
				.andExpect(jsonPath("$.paths['/auth/login'].post.responses.400").exists())
				.andExpect(jsonPath("$.paths['/auth/login'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/auth/logout'].post.responses.204").exists())
				.andExpect(jsonPath("$.paths['/auth/logout'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/auth/logout'].post.parameters").doesNotExist())
				.andExpect(jsonPath("$.components.securitySchemes.bearer.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"))
				.andExpect(
						jsonPath("$.components.schemas.EntityModelLoginResponse.properties.token_type").exists())
				.andExpect(
						jsonPath("$.components.schemas.EntityModelLoginResponse.properties.expires_at").exists());
	}

	private MockHttpServletRequestBuilder login(String email, String password) {
		return post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password));
	}

	private String tokenOf(MockHttpServletRequestBuilder request) throws Exception {
		String body = mockMvc.perform(request)
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private String createAna() throws Exception {
		return createAccount("ana@exemplo.com", "Ana", "segredo");
	}

	private String createAccount(String email, String displayName, String password) throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"%s\", \"display_name\": \"%s\", \"password\": \"%s\"}"
								.formatted(email, displayName, password)))
				.andExpect(status().isCreated());
		return jdbcTemplate.queryForObject("select id from accounts where email = ?", UUID.class, email).toString();
	}

	private Integer countTokens() {
		return jdbcTemplate.queryForObject("select count(*) from auth_tokens", Integer.class);
	}

}
