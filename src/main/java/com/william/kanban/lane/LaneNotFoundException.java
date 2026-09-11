package com.william.kanban.lane;

import java.util.UUID;

public class LaneNotFoundException extends RuntimeException {

	LaneNotFoundException(UUID id) {
		super("Lane não encontrada: " + id);
	}

}
