package com.william.kanban.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

record AccountResponse(

		UUID id,

		String email,

		@JsonProperty("display_name")
		String displayName

) {
}
