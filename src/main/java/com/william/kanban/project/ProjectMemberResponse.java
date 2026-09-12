package com.william.kanban.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "members")
record ProjectMemberResponse(

		UUID id,

		@JsonProperty("account_id")
		UUID accountId,

		String email,

		@JsonProperty("display_name")
		String displayName,

		Set<ProjectPermission> permissions,

		@JsonProperty("created_at")
		OffsetDateTime createdAt

) {
}
