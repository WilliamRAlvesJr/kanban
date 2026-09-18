package com.william.kanban.shared;

import com.william.kanban.board.BoardNotFoundException;
import com.william.kanban.exception.AccountNotFoundException;
import com.william.kanban.exception.InvalidCredentialsException;
import com.william.kanban.lane.LaneNotFoundException;
import com.william.kanban.lane.LaneOrderMismatchException;
import com.william.kanban.project.ProjectAccessDeniedException;
import com.william.kanban.project.ProjectMemberNotFoundException;
import com.william.kanban.project.ProjectNotFoundException;
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

	@ExceptionHandler({AccountNotFoundException.class, BoardNotFoundException.class,
			LaneNotFoundException.class, ProjectMemberNotFoundException.class, ProjectNotFoundException.class})
	ProblemDetail handleNotFound(RuntimeException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ProjectAccessDeniedException.class)
	ProblemDetail handleAccessDenied(ProjectAccessDeniedException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
	}

	@ExceptionHandler(LaneOrderMismatchException.class)
	ProblemDetail handleLaneOrderMismatch(LaneOrderMismatchException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

}
