package com.william.kanban.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record AccountResponse(

		UUID id,

		String email,

		@JsonProperty("display_name")
		String displayName

) {
}
