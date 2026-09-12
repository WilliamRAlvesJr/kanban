package com.william.kanban.lane;

import java.util.List;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.stereotype.Component;

@Component
class LaneModelAssembler {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

	EntityModel<LaneResponse> toModel(Lane lane) {
		String self = hrefOf(lane);
		EntityModel<LaneResponse> model = EntityModel.of(toResponse(lane), Link.of(self), Link.of(self, "edit"));
		model.add(lane.getArchivedAt() == null
				? Link.of(self + "/archive", "archive")
				: Link.of(self + "/restore", "restore"));
		return model.add(Link.of(boardHrefOf(lane.getBoardId()), "board"));
	}

	/** CollectionModel sem item omite _embedded; o wrapper vazio mantém o array. */
	CollectionModel<?> toCollection(List<Lane> lanes, UUID boardId) {
		CollectionModel<?> collection = lanes.isEmpty()
				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(LaneResponse.class)))
				: CollectionModel.of(lanes.stream().map(this::toModel).toList());
		String lanesHref = lanesHrefOf(boardId);
		return collection.add(
				Link.of(lanesHref),
				Link.of(lanesHref, "create-lane"),
				Link.of(lanesHref + "/order", "reorder-lanes"),
				Link.of(boardHrefOf(boardId), "board"));
	}

	RepresentationModel<?> selfOf(Lane lane) {
		return new RepresentationModel<>(Link.of(hrefOf(lane)));
	}

	RepresentationModel<?> selfOfCollection(UUID boardId) {
		return new RepresentationModel<>(Link.of(lanesHrefOf(boardId)));
	}

	private static String hrefOf(Lane lane) {
		return lanesHrefOf(lane.getBoardId()) + "/" + lane.getId();
	}

	private static String lanesHrefOf(UUID boardId) {
		return boardHrefOf(boardId) + "/lanes";
	}

	private static String boardHrefOf(UUID boardId) {
		return "/boards/" + boardId;
	}

	private static LaneResponse toResponse(Lane lane) {
		return new LaneResponse(lane.getId(), lane.getName(), lane.getPosition(),
				lane.getCreatedAt(), lane.getUpdatedAt(), lane.getArchivedAt());
	}

}
