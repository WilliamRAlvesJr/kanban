package com.william.kanban.board;

import java.util.List;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.stereotype.Component;

@Component
class BoardModelAssembler {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

	EntityModel<BoardResponse> toModel(Board board) {
		String self = "/boards/" + board.getId();
		EntityModel<BoardResponse> model = EntityModel.of(toResponse(board), Link.of(self), Link.of(self, "edit"));
		model.add(board.getArchivedAt() == null
				? Link.of(self + "/archive", "archive")
				: Link.of(self + "/restore", "restore"));
		return model.add(
				Link.of(self + "/move", "move"),
				Link.of("/projects/" + board.getProjectId(), "project"),
				Link.of(self + "/lanes", "lanes"),
				Link.of(self + "/lanes", "create-lane"),
				Link.of(self + "/lanes/order", "reorder-lanes"));
	}

	/** CollectionModel sem item omite _embedded; o wrapper vazio mantém o array. */
	CollectionModel<?> toCollection(List<Board> boards, UUID projectId) {
		CollectionModel<?> collection = boards.isEmpty()
				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(BoardResponse.class)))
				: CollectionModel.of(boards.stream().map(this::toModel).toList());
		String project = "/projects/" + projectId;
		return collection.add(
				Link.of(project + "/boards"),
				Link.of(project + "/boards", "create-board"),
				Link.of(project, "project"));
	}

	RepresentationModel<?> selfOf(Board board) {
		return new RepresentationModel<>(Link.of("/boards/" + board.getId()));
	}

	private static BoardResponse toResponse(Board board) {
		return new BoardResponse(board.getId(), board.getProjectId(), board.getName(), board.getDescription(),
				board.getCreatedAt(), board.getUpdatedAt(), board.getArchivedAt());
	}

}
