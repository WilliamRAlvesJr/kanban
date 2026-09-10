package com.william.kanban.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record CreateAccountRequest(

		@NotBlank
		@Email
		String email,

		@JsonProperty("display_name")
		@NotBlank
		String displayName,

		@NotBlank
		String password

) {
}
