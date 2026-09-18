package com.william.kanban.support;

import static com.william.kanban.support.ApiPaths.ACCOUNTS;
import static com.william.kanban.support.ApiPaths.LOGIN;
import static org.springframework.http.HttpStatus.CREATED;

import com.william.kanban.auth.LoginRequest;
import com.william.kanban.dto.account.CreateAccountRequest;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class AccountFixture {

	public static final CreateAccountRequest ANA =
		new CreateAccountRequest("ana@exemplo.com", "Ana", "segredo");

	public static final LoginRequest ANA_LOGIN =
		new LoginRequest("ana@exemplo.com", "segredo");

	private final ApiClient api;

	private final JdbcTemplate jdbcTemplate;

	public AccountFixture(ApiClient api, JdbcTemplate jdbcTemplate) {
		this.api = api;
		this.jdbcTemplate = jdbcTemplate;
	}

	public String createAccount(CreateAccountRequest account) {
		api.post(ACCOUNTS)
			.withBody(account)
			.perform()
			.expectStatus(CREATED);
		return jdbcTemplate.queryForObject(
			"select id from accounts where email = ?", UUID.class, account.email()
		).toString();
	}

	public String createAna() {
		return createAccount(ANA);
	}

	public String tokenOf(LoginRequest login) {
		return api.post(LOGIN)
			.withBody(login)
			.perform()
			.expectStatus(CREATED)
			.json("$.token");
	}

}
