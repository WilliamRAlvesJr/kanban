package com.william.kanban.project;

import java.util.Locale;
import java.util.UUID;

public class ProjectAccessDeniedException extends RuntimeException {

	ProjectAccessDeniedException(UUID projectId, ProjectPermission permission) {
		super("Permissão " + permission.name().toLowerCase(Locale.ROOT) + " ausente no projeto " + projectId);
	}

}
