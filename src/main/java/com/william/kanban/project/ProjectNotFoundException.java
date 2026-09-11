package com.william.kanban.project;

import java.util.UUID;

public class ProjectNotFoundException extends RuntimeException {

	ProjectNotFoundException(UUID id) {
		super("Projeto não encontrado: " + id);
	}

}
