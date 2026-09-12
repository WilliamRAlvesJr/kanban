package com.william.kanban.project;

import jakarta.validation.constraints.NotNull;
import java.util.List;

record UpdateProjectMemberPermissionsRequest(

		@NotNull
		List<@NotNull ProjectPermission> permissions

) {
}
