package com.william.kanban.exception;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Email ou senha inválidos.");
	}

}
