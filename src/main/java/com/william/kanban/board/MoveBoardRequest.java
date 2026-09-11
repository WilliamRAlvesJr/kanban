package com.william.kanban.board;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record MoveBoardRequest(

		@NotNull
		@JsonProperty("project_id")
		UUID projectId

) {
}
