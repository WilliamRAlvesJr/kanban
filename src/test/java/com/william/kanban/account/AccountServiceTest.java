package com.william.kanban.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.william.kanban.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountServiceTest {

	@Autowired
	AccountService service;

	@Autowired
	AccountRepository repository;

	@Test
	void findByIdRejectsUnknownId() {
		UUID id = UUID.randomUUID();

		assertThatThrownBy(() -> service.findById(id))
				.isInstanceOf(AccountNotFoundException.class)
				.hasMessageContaining(id.toString());
	}

	@Test
	void unknownEmailComparesPasswordAgainstHashOfEncoderCost() {
		BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
		List<String> comparedHashes = new ArrayList<>();
		PasswordEncoder encoder = new PasswordEncoder() {

			@Override
			public String encode(CharSequence rawPassword) {
				return bcrypt.encode(rawPassword);
			}

			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				comparedHashes.add(encodedPassword);
				return bcrypt.matches(rawPassword, encodedPassword);
			}

		};

		new AccountService(repository, encoder).authenticate("ninguem@exemplo.com", "segredo");

		assertThat(comparedHashes).singleElement().asString().startsWith("$2a$04$");
	}

}
