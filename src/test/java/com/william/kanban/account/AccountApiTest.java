package com.william.kanban.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void createsAccount() throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/accounts/[0-9a-f-]{36}$")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.email").value("ana@exemplo.com"))
				.andExpect(jsonPath("$.display_name").value("Ana"));
	}

	@Test
	void normalizesEmailToLowercase() throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "Ana@Exemplo.com", "display_name": "Ana", "password": "segredo"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("ana@exemplo.com"));

		assertThat(jdbcTemplate.queryForObject("select email from accounts", String.class))
				.isEqualTo("ana@exemplo.com");
	}

	@Test
	void storesPasswordAsBcryptHash() throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}
								"""))
				.andExpect(status().isCreated());

		String passwordHash = jdbcTemplate.queryForObject("select password_hash from accounts", String.class);
		assertThat(passwordHash).isNotEqualTo("segredo");
		assertThat(new BCryptPasswordEncoder().matches("segredo", passwordHash)).isTrue();
	}

	@Test
	void doesNotReturnPassword() throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.password_hash").doesNotExist());
	}

	@Test
	void databaseRejectsEmailNotNormalized() {
		assertThatThrownBy(() -> jdbcTemplate.update(
				"insert into accounts (id, email, display_name, password_hash) values (?, ?, ?, ?)",
				UUID.randomUUID(), "Ana@Exemplo.com", "Ana", "hash"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"""
					{"display_name": "Ana", "password": "segredo"}""",
			"""
					{"email": "ana@exemplo.com", "password": "segredo"}""",
			"""
					{"email": "ana@exemplo.com", "display_name": "Ana"}""",
			"""
					{"email": "", "display_name": "Ana", "password": "segredo"}""",
			"""
					{"email": "ana@exemplo.com", "display_name": "", "password": "segredo"}""",
			"""
					{"email": "ana@exemplo.com", "display_name": "Ana", "password": ""}"""
	})
	void rejectsPayloadWithRequiredFieldMissingOrBlank(String body) throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(countAccounts()).isZero();
	}

	@Test
	void rejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana.exemplo.com", "display_name": "Ana", "password": "segredo"}
								"""))
				.andExpect(status().isBadRequest());

		assertThat(countAccounts()).isZero();
	}

	@Test
	void rejectsDuplicateEmail() throws Exception {
		createAna();

		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana@exemplo.com", "display_name": "Outra Ana", "password": "outra"}
								"""))
				.andExpect(status().isConflict());

		assertThat(countAccounts()).isOne();
	}

	@Test
	void rejectsDuplicateEmailInAnotherCase() throws Exception {
		createAna();

		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ANA@exemplo.com", "display_name": "Outra Ana", "password": "outra"}
								"""))
				.andExpect(status().isConflict());

		assertThat(countAccounts()).isOne();
	}

	@Test
	void findsAccountById() throws Exception {
		String id = createAna();

		mockMvc.perform(get("/accounts/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.email").value("ana@exemplo.com"))
				.andExpect(jsonPath("$.display_name").value("Ana"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.password_hash").doesNotExist());
	}

	@Test
	void returnsNotFoundForUnknownId() throws Exception {
		mockMvc.perform(get("/accounts/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsIdThatIsNotUuid() throws Exception {
		mockMvc.perform(get("/accounts/{id}", "nao-e-uuid"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void documentsAccountEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/accounts'].post.responses.201").exists())
				.andExpect(jsonPath("$.paths['/accounts'].post.responses.400").exists())
				.andExpect(jsonPath("$.paths['/accounts'].post.responses.409").exists())
				.andExpect(jsonPath("$.paths['/accounts'].post.responses.200").doesNotExist())
				.andExpect(jsonPath("$.paths['/accounts/{id}'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/accounts/{id}'].get.responses.400").exists())
				.andExpect(jsonPath("$.paths['/accounts/{id}'].get.responses.404").exists())
				.andExpect(jsonPath("$.components.schemas.CreateAccountRequest.properties.display_name").exists())
				.andExpect(jsonPath("$.components.schemas.AccountResponse.properties.display_name").exists());
	}

	private String createAna() throws Exception {
		String body = mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private Integer countAccounts() {
		return jdbcTemplate.queryForObject("select count(*) from accounts", Integer.class);
	}

}
