package com.william.kanban.auth;

public class InvalidCredentialsException extends RuntimeException {

	InvalidCredentialsException() {
		super("Email ou senha inválidos.");
	}

}
