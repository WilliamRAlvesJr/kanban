package com.william.kanban.project;

import java.util.UUID;

public class ProjectMemberNotFoundException extends RuntimeException {

	ProjectMemberNotFoundException(UUID id) {
		super("Membro não encontrado: " + id);
	}

}
