package com.william.kanban.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record AddProjectMemberRequest(

		@NotBlank
		@Email
		String email

) {
}
