package com.william.kanban.board;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "boards")
record BoardResponse(

		UUID id,

		@JsonProperty("project_id")
		UUID projectId,

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
