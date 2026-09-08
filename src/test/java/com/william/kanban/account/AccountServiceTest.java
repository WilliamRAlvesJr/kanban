package com.william.kanban.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.william.kanban.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountServiceTest {

	@Autowired
	AccountService service;

	@Test
	void findByIdRejectsUnknownId() {
		UUID id = UUID.randomUUID();

		assertThatThrownBy(() -> service.findById(id))
				.isInstanceOf(AccountNotFoundException.class)
				.hasMessageContaining(id.toString());
	}

}
