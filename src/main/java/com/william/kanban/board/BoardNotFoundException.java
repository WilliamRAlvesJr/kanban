package com.william.kanban.board;

import java.util.UUID;

public class BoardNotFoundException extends RuntimeException {

	BoardNotFoundException(UUID id) {
		super("Quadro não encontrado: " + id);
	}

}
