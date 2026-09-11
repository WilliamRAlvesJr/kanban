package com.william.kanban.lane;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "lanes")
record LaneResponse(

		UUID id,

		String name,

		Integer position,

		@JsonProperty("created_at")
		OffsetDateTime createdAt,

		@JsonProperty("updated_at")
		OffsetDateTime updatedAt,

		@JsonProperty("archived_at")
		OffsetDateTime archivedAt

) {
}
