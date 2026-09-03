package com.william.kanban.shared;

import com.william.kanban.account.AccountNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String UNIQUE_EMAIL_INDEX = "ux_accounts_email";

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleConflict(DataIntegrityViolationException e) {
		if (!(e.getCause() instanceof ConstraintViolationException violation)
				|| !UNIQUE_EMAIL_INDEX.equals(violation.getConstraintName())) {
			throw e;
		}
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email já cadastrado.");
	}

	@ExceptionHandler(AccountNotFoundException.class)
	ProblemDetail handleNotFound(AccountNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

}
