package com.william.kanban.lane;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

record ReorderLanesRequest(

		@NotNull
		@JsonProperty("lane_ids")
		List<@NotNull UUID> laneIds

) {

	@JsonIgnore
	@AssertTrue
	boolean isLaneIdsDistinct() {
		return laneIds == null || new HashSet<>(laneIds).size() == laneIds.size();
	}

}
