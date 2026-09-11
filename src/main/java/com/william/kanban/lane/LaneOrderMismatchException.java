package com.william.kanban.lane;

public class LaneOrderMismatchException extends RuntimeException {

	LaneOrderMismatchException() {
		super("A lista tem de trazer exatamente as lanes ativas do quadro.");
	}

}
