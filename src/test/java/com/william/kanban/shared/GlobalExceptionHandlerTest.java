package com.william.kanban.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void mapsDuplicateEmailToConflict() {
		ProblemDetail problem = handler.handleConflict(violationOf("ux_accounts_email"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getDetail()).isEqualTo("Email já cadastrado.");
	}

	@Test
	void rethrowsViolationOfAnotherConstraint() {
		DataIntegrityViolationException e = violationOf("ck_accounts_email_lowercase");

		assertThatThrownBy(() -> handler.handleConflict(e)).isSameAs(e);
	}

	@Test
	void rethrowsWhenCauseIsNotConstraintViolation() {
		DataIntegrityViolationException e = new DataIntegrityViolationException("falha", new SQLException("falha"));

		assertThatThrownBy(() -> handler.handleConflict(e)).isSameAs(e);
	}

	private DataIntegrityViolationException violationOf(String constraintName) {
		return new DataIntegrityViolationException("falha",
				new ConstraintViolationException("falha", new SQLException("falha"), constraintName));
	}

}
