package com.william.kanban.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

record ProjectResponse(

		UUID id,

		String name,

		String description,

		@JsonProperty("created_at")
		OffsetDateTime createdAt,

		@JsonProperty("updated_at")
		OffsetDateTime updatedAt,

		@JsonProperty("archived_at")
		OffsetDateTime archivedAt

) {
}
