package com.william.kanban.board;

import com.william.kanban.project.ProjectAccess;
import com.william.kanban.project.ProjectPermission;
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

	EntityModel<BoardResponse> toModel(Board board, ProjectAccess access) {
		String self = "/boards/" + board.getId();
		EntityModel<BoardResponse> model = EntityModel.of(toResponse(board), Link.of(self));
		if (access.owner()) {
			model.add(Link.of(self, "edit"));
			model.add(board.getArchivedAt() == null
					? Link.of(self + "/archive", "archive")
					: Link.of(self + "/restore", "restore"));
			model.add(Link.of(self + "/move", "move"));
		}
		if (access.allows(ProjectPermission.VIEW_PROJECT)) {
			model.add(Link.of("/projects/" + board.getProjectId(), "project"));
		}
		if (access.owner()) {
			model.add(
					Link.of(self + "/lanes", "lanes"),
					Link.of(self + "/lanes", "create-lane"),
					Link.of(self + "/lanes/order", "reorder-lanes"));
		}
		return model;
	}

	/** CollectionModel sem item omite _embedded; o wrapper vazio mantém o array. */
	CollectionModel<?> toCollection(ProjectBoards boards, UUID projectId) {
		CollectionModel<?> collection = boards.boards().isEmpty()
				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(BoardResponse.class)))
				: CollectionModel.of(boards.boards().stream().map(board -> toModel(board, boards.access())).toList());
		String project = "/projects/" + projectId;
		collection.add(Link.of(project + "/boards"));
		if (boards.access().allows(ProjectPermission.ADD_BOARDS)) {
			collection.add(Link.of(project + "/boards", "create-board"));
		}
		if (boards.access().allows(ProjectPermission.VIEW_PROJECT)) {
			collection.add(Link.of(project, "project"));
		}
		return collection;
	}

	RepresentationModel<?> selfOf(Board board) {
		return new RepresentationModel<>(Link.of("/boards/" + board.getId()));
	}

	private static BoardResponse toResponse(Board board) {
		return new BoardResponse(board.getId(), board.getProjectId(), board.getName(), board.getDescription(),
				board.getCreatedAt(), board.getUpdatedAt(), board.getArchivedAt());
	}

}
