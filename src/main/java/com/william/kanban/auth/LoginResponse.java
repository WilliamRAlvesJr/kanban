package com.william.kanban.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

record LoginResponse(

		String token,

		@JsonProperty("token_type")
		String tokenType,

		@JsonProperty("expires_at")
		OffsetDateTime expiresAt

) {
}
