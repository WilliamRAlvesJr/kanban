package com.william.kanban.dto.account;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.With;

@With
@JsonInclude(NON_NULL)
public record CreateAccountRequest(

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
