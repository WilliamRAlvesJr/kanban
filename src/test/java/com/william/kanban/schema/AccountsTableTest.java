package com.william.kanban.schema;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.william.kanban.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountsTableTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void databaseRejectsEmailNotNormalized() {
		var id = UUID.randomUUID();

		assertThatThrownBy(() -> jdbcTemplate.update(
			"""
				insert into accounts (id, email, display_name, password_hash)
				values (?, ?, ?, ?)
			""",
			id, "Ana@Exemplo.com", "Ana", "hash"
		))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
