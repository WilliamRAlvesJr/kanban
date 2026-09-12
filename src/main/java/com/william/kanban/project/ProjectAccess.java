package com.william.kanban.project;

import java.util.Set;

public record ProjectAccess(boolean owner, Set<ProjectPermission> permissions) {

	public boolean allows(ProjectPermission permission) {
		return owner || permissions.contains(permission);
	}

}
