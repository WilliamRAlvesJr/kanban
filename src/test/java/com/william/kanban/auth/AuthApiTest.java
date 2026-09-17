package com.william.kanban.auth;

import static com.william.kanban.support.ApiClient.link;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.william.kanban.TestcontainersConfiguration;
import com.william.kanban.dto.account.CreateAccountRequest;
import com.william.kanban.support.ApiClient;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiTest {

	private static final String LOGIN = "/auth/login";

	private static final String LOGOUT = "/auth/logout";

	private static final String ME = "/accounts/me";

	private static final String ACCOUNTS = "/accounts";

	private static final CreateAccountRequest ANA =
		new CreateAccountRequest("ana@exemplo.com", "Ana", "segredo");

	private static final LoginRequest ANA_LOGIN =
		new LoginRequest("ana@exemplo.com", "segredo");

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
	void issuesTokenForCorrectCredentials() {
		createAna();

		api.post(LOGIN)
			.withBody(ANA_LOGIN)
			.perform()
			.expectStatus(CREATED)
			.expectJson("$.token", not(emptyOrNullString()))
			.expectJson("$.token_type", "Bearer")
			.expectJson("$.expires_at", not(emptyOrNullString()))
			.expectLinks(
				link("me", ME),
				link("logout", LOGOUT)
			)
			.expectNoLocation();
	}

	@Test
	void issuesTokenForEmailInAnotherCase() {
		createAna();

		api.post(LOGIN)
			.withBody(ANA_LOGIN.withEmail("ANA@Exemplo.com"))
			.perform()
			.expectStatus(CREATED)
			.expectJson("$.token", not(emptyOrNullString()))
			.expectJson("$.token_type", "Bearer");
	}

	static Stream<LoginRequest> incompleteLogins() {
		return Stream.of(
			ANA_LOGIN.withEmail(null),
			ANA_LOGIN.withPassword(null),
			ANA_LOGIN.withEmail(""),
			ANA_LOGIN.withPassword("")
		);
	}

	@ParameterizedTest
	@MethodSource("incompleteLogins")
	void rejectsPayloadWithRequiredFieldMissingOrBlank(LoginRequest body) {
		createAna();

		api.post(LOGIN)
			.withBody(body)
			.perform()
			.expectStatus(BAD_REQUEST);

		assertThat(countTokens()).isZero();
	}

	@Test
	void rejectsWrongPassword() {
		createAna();

		api.post(LOGIN)
			.withBody(ANA_LOGIN.withPassword("errada"))
			.perform()
			.expectStatus(UNAUTHORIZED);

		assertThat(countTokens()).isZero();
	}

	@Test
	void answersUnknownEmailWithTheSameBodyAsWrongPassword() {
		createAna();

		var wrongPassword = api.post(LOGIN)
			.withBody(ANA_LOGIN.withPassword("errada"))
			.perform()
			.expectStatus(UNAUTHORIZED)
			.body();
		var unknownEmail = api.post(LOGIN)
			.withBody(ANA_LOGIN.withEmail("bruno@exemplo.com"))
			.perform()
			.expectStatus(UNAUTHORIZED)
			.body();

		assertThat(unknownEmail).isEqualTo(wrongPassword);
		assertThat(countTokens()).isZero();
	}

	@Test
	void issuesIndependentTokenOnEachLogin() {
		createAna();
		var first = tokenOf(ANA_LOGIN);
		var second = tokenOf(ANA_LOGIN);

		assertThat(second).isNotEqualTo(first);
		api.get(ME)
			.withToken(first)
			.perform()
			.expectStatus(OK);
	}

	@Test
	void storesOnlyTheHashOfTheToken() {
		createAna();
		var token = tokenOf(ANA_LOGIN);

		var tokenHash = jdbcTemplate.queryForObject(
			"select token_hash from auth_tokens", String.class
		);
		assertThat(tokenHash).isNotEqualTo(token);
		var matches = jdbcTemplate.queryForObject(
			"select count(*) from auth_tokens where token_hash = ?",
			Integer.class,
			token
		);
		assertThat(matches).isZero();
	}

	@Test
	void acceptsValidToken() {
		var id = createAna();
		var token = tokenOf(ANA_LOGIN);

		api.get(ME)
			.withToken(token)
			.perform()
			.expectStatus(OK)
			.expectJson("$.id", id)
			.expectJson("$.email", "ana@exemplo.com");
	}

	@Test
	void returnsTheAccountOfTheTokenUsed() {
		createAna();
		var brunoId = createAccount(ANA
			.withEmail("bruno@exemplo.com")
			.withDisplayName("Bruno")
			.withPassword("outra")
		);
		var brunoToken = tokenOf(ANA_LOGIN
			.withEmail("bruno@exemplo.com")
			.withPassword("outra")
		);

		api.get(ME)
			.withToken(brunoToken)
			.perform()
			.expectStatus(OK)
			.expectJson("$.id", brunoId)
			.expectJson("$.email", "bruno@exemplo.com");
	}

	@Test
	void acceptsTokenIssuedAnHourAgo() {
		createAna();
		var token = tokenOf(ANA_LOGIN);
		jdbcTemplate.update(
			"update auth_tokens set created_at = now() - interval '1 hour',"
				+ " expires_at = expires_at - interval '1 hour'"
		);

		api.get(ME)
			.withToken(token)
			.perform()
			.expectStatus(OK);
	}

	@Test
	void rejectsRequestWithoutAuthorizationHeader() {
		api.get(ME)
			.perform()
			.expectStatus(UNAUTHORIZED)
			.expectJson("$.status", 401)
			.expectJson("$.detail", not(emptyOrNullString()))
			.expectJson("$.instance", ME);
	}

	@Test
	void rejectsAuthorizationHeaderInAnotherFormat() {
		api.get(ME)
			.withHeader(AUTHORIZATION, "Basic YWJj")
			.perform()
			.expectStatus(UNAUTHORIZED)
			.expectJson("$.status", 401);
	}

	@Test
	void rejectsValidTokenUnderAnotherScheme() {
		createAna();
		var token = tokenOf(ANA_LOGIN);

		api.get(ME)
			.withHeader(AUTHORIZATION, "Digest " + token)
			.perform()
			.expectStatus(UNAUTHORIZED)
			.expectJson("$.status", 401);
	}

	@Test
	void rejectsUnknownToken() {
		api.get(ME)
			.withToken("nunca-emitido")
			.perform()
			.expectStatus(UNAUTHORIZED)
			.expectJson("$.status", 401);
	}

	@Test
	void rejectsExpiredToken() {
		createAna();
		var token = tokenOf(ANA_LOGIN);
		jdbcTemplate.update(
			"update auth_tokens set expires_at = now() - interval '1 minute'"
		);

		api.get(ME)
			.withToken(token)
			.perform()
			.expectStatus(UNAUTHORIZED);
	}

	@Test
	void logoutRevokesTheTokenUsed() {
		createAna();
		var token = tokenOf(ANA_LOGIN);

		api.post(LOGOUT)
			.withToken(token)
			.perform()
			.expectStatus(NO_CONTENT);
		api.get(ME)
			.withToken(token)
			.perform()
			.expectStatus(UNAUTHORIZED);
	}

	@Test
	void logoutKeepsTheOtherTokens() {
		createAna();
		var first = tokenOf(ANA_LOGIN);
		var second = tokenOf(ANA_LOGIN);

		api.post(LOGOUT)
			.withToken(first)
			.perform()
			.expectStatus(NO_CONTENT);
		api.get(ME)
			.withToken(second)
			.perform()
			.expectStatus(OK);
	}

	@Test
	void rejectsLogoutWithoutToken() {
		api.post(LOGOUT)
			.perform()
			.expectStatus(UNAUTHORIZED);
	}

	@Test
	void documentsAuthEndpoints() {
		api.get("/v3/api-docs")
			.perform()
			.expectStatus(OK)
			.expectPresent("$.paths['/auth/login'].post.responses.201")
			.expectPresent("$.paths['/auth/login'].post.responses.400")
			.expectPresent("$.paths['/auth/login'].post.responses.401")
			.expectPresent("$.paths['/auth/logout'].post.responses.204")
			.expectPresent("$.paths['/auth/logout'].post.responses.401")
			.expectAbsent("$.paths['/auth/logout'].post.parameters")
			.expectJson("$.components.securitySchemes.bearer.type", "http")
			.expectJson("$.components.securitySchemes.bearer.scheme", "bearer")
			.expectPresent(
				"$.components.schemas.EntityModelLoginResponse.properties.token_type"
			)
			.expectPresent(
				"$.components.schemas.EntityModelLoginResponse.properties.expires_at"
			);
	}

	private String createAccount(CreateAccountRequest account) {
		api.post(ACCOUNTS)
			.withBody(account)
			.perform()
			.expectStatus(CREATED);
		return jdbcTemplate.queryForObject(
			"select id from accounts where email = ?", UUID.class, account.email()
		).toString();
	}

	private String createAna() {
		return createAccount(ANA);
	}

	private String tokenOf(LoginRequest login) {
		return api.post(LOGIN)
			.withBody(login)
			.perform()
			.expectStatus(CREATED)
			.json("$.token");
	}

	private Integer countTokens() {
		return jdbcTemplate.queryForObject(
			"select count(*) from auth_tokens", Integer.class
		);
	}

}
