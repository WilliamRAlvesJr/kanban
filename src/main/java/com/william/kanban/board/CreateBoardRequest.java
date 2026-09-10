package com.william.kanban.board;

import static com.william.kanban.board.BoardLimits.DESCRIPTION_MAX_LENGTH;
import static com.william.kanban.board.BoardLimits.NAME_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateBoardRequest(

		@NotBlank
		@Size(max = NAME_MAX_LENGTH)
		String name,

		@Size(max = DESCRIPTION_MAX_LENGTH)
		String description

) {
}
