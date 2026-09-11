package com.william.kanban.lane;

import static com.william.kanban.lane.LaneLimits.NAME_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateLaneRequest(

		@NotBlank
		@Size(max = NAME_MAX_LENGTH)
		String name

) {
}
