package com.william.kanban.auth;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.With;

@With
@JsonInclude(NON_NULL)
public record LoginRequest(

		@NotBlank
		String email,

		@NotBlank
		String password

) {
}
