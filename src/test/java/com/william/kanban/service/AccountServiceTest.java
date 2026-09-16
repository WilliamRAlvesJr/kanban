package com.william.kanban.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.william.kanban.TestcontainersConfiguration;
import com.william.kanban.exception.AccountNotFoundException;
import com.william.kanban.mapper.AccountMapper;
import com.william.kanban.repository.AccountRepository;
import java.util.ArrayList;
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

	@Autowired
	AccountMapper mapper;

	@Test
	void findByIdRejectsUnknownId() {
		var id = UUID.randomUUID();

		assertThatThrownBy(() -> service.findById(id))
			.isInstanceOf(AccountNotFoundException.class)
			.hasMessageContaining(id.toString());
	}

	@Test
	void unknownEmailComparesPasswordAgainstHashOfEncoderCost() {
		var bcrypt = new BCryptPasswordEncoder(4);
		var comparedHashes = new ArrayList<String>();
		var encoder = new PasswordEncoder() {

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

		new AccountService(repository, encoder, mapper)
			.authenticate("ninguem@exemplo.com", "segredo");

		assertThat(comparedHashes).singleElement().asString().startsWith("$2a$04$");
	}

}
