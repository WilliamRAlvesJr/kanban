package com.william.kanban.project;

import static com.william.kanban.project.ProjectLimits.DESCRIPTION_MAX_LENGTH;
import static com.william.kanban.project.ProjectLimits.NAME_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateProjectRequest(

		@NotBlank
		@Size(max = NAME_MAX_LENGTH)
		String name,

		@Size(max = DESCRIPTION_MAX_LENGTH)
		String description

) {
}
