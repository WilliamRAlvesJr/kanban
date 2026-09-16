package com.william.kanban.controller;

import static com.william.kanban.support.ApiClient.link;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.william.kanban.TestcontainersConfiguration;
import com.william.kanban.auth.LoginRequest;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountApiTest {

	private static final String ACCOUNTS = "/accounts";

	private static final String ME = "/accounts/me";

	private static final String LOGIN = "/auth/login";

	private static final CreateAccountRequest ANA =
		new CreateAccountRequest("ana@exemplo.com", "Ana", "segredo");

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
	void createsAccount() {
		api.post(ACCOUNTS)
			.withBody(ANA)
			.perform()
			.expectStatus(CREATED)
			.expectLocation(ME)
			.expectJson("$", aMapWithSize(1))
			.expectLinks(
				link("self", ME),
				link("login", LOGIN)
			);
	}

	@Test
	void normalizesEmailToLowercase() {
		api.post(ACCOUNTS)
			.withBody(ANA.withEmail("Ana@Exemplo.com"))
			.perform()
			.expectStatus(CREATED);

		var email = jdbcTemplate.queryForObject(
			"select email from accounts", String.class
		);
		assertThat(email).isEqualTo("ana@exemplo.com");
	}

	@Test
	void storesPasswordAsBcryptHash() {
		api.post(ACCOUNTS)
			.withBody(ANA)
			.perform()
			.expectStatus(CREATED);

		var passwordHash = jdbcTemplate.queryForObject(
			"select password_hash from accounts", String.class
		);
		assertThat(passwordHash).isNotEqualTo("segredo");
		assertThat(new BCryptPasswordEncoder().matches("segredo", passwordHash)).isTrue();
	}

	@Test
	void doesNotReturnPassword() {
		var id = createAna();

		api.get(ME)
			.withToken(loginAsAna())
			.perform()
			.expectStatus(OK)
			.expectJson("$", aMapWithSize(4))
			.expectJson("$.id", id)
			.expectPresent("$.email")
			.expectPresent("$.display_name")
			.expectPresent("$._links")
			.expectAbsent("$.password")
			.expectAbsent("$.password_hash");
	}

	@Test
	void returnsAccountOfTokenWithLinks() {
		var id = createAna();

		api.get(ME)
			.withToken(loginAsAna())
			.perform()
			.expectStatus(OK)
			.expectJson("$.id", id)
			.expectJson("$.email", "ana@exemplo.com")
			.expectJson("$.display_name", "Ana")
			.expectLinks(
				link("self", ME),
				link("projects", "/projects")
			);
	}

	static Stream<CreateAccountRequest> incompleteAccounts() {
		return Stream.of(
			ANA.withEmail(null),
			ANA.withDisplayName(null),
			ANA.withPassword(null),
			ANA.withEmail(""),
			ANA.withDisplayName(""),
			ANA.withPassword("")
		);
	}

	@ParameterizedTest
	@MethodSource("incompleteAccounts")
	void rejectsPayloadWithRequiredFieldMissingOrBlank(CreateAccountRequest body) {
		api.post(ACCOUNTS)
			.withBody(body)
			.perform()
			.expectStatus(BAD_REQUEST);

		assertThat(countAccounts()).isZero();
	}

	@Test
	void rejectsInvalidEmail() {
		api.post(ACCOUNTS)
			.withBody(ANA.withEmail("ana.exemplo.com"))
			.perform()
			.expectStatus(BAD_REQUEST);

		assertThat(countAccounts()).isZero();
	}

	@Test
	void rejectsDuplicateEmail() {
		createAna();

		api.post(ACCOUNTS)
			.withBody(ANA.withDisplayName("Outra Ana").withPassword("outra"))
			.perform()
			.expectStatus(CONFLICT);

		assertThat(countAccounts()).isOne();
	}

	@Test
	void rejectsDuplicateEmailInAnotherCase() {
		createAna();

		api.post(ACCOUNTS)
			.withBody(ANA
				.withEmail("ANA@exemplo.com")
				.withDisplayName("Outra Ana")
				.withPassword("outra")
			)
			.perform()
			.expectStatus(CONFLICT);

		assertThat(countAccounts()).isOne();
	}

	@Test
	void documentsAccountEndpoints() {
		api.get("/v3/api-docs")
			.perform()
			.expectStatus(OK)
			.expectPresent("$.paths['/accounts'].post.responses.201")
			.expectPresent("$.paths['/accounts'].post.responses.400")
			.expectPresent("$.paths['/accounts'].post.responses.409")
			.expectAbsent("$.paths['/accounts'].post.responses.200")
			.expectAbsent("$.paths['/accounts/{id}']")
			.expectPresent("$.paths['/accounts/me'].get.responses.200")
			.expectPresent("$.paths['/accounts/me'].get.responses.401")
			.expectPresent(
				"$.components.schemas.CreateAccountRequest.properties.display_name"
			)
			.expectPresent(
				"$.components.schemas.EntityModelAccountResponse.properties.display_name"
			);
	}

	private String createAna() {
		api.post(ACCOUNTS)
			.withBody(ANA)
			.perform()
			.expectStatus(CREATED);
		return jdbcTemplate.queryForObject(
			"select id from accounts where email = ?", UUID.class, "ana@exemplo.com"
		).toString();
	}

	private String loginAsAna() {
		return api.post(LOGIN)
			.withBody(new LoginRequest("ana@exemplo.com", "segredo"))
			.perform()
			.expectStatus(CREATED)
			.json("$.token");
	}

	private Integer countAccounts() {
		return jdbcTemplate.queryForObject(
			"select count(*) from accounts", Integer.class
		);
	}

}
