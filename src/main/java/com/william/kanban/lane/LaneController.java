package com.william.kanban.lane;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/boards/{boardId}/lanes")
class LaneController {

	private final LaneService service;

	LaneController(LaneService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	LaneResponse create(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@Valid
			@RequestBody
			CreateLaneRequest request

	) {
		return toResponse(service.create(boardId, accountId, request.name()));
	}

	@PutMapping("/{laneId}")
	LaneResponse rename(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@PathVariable
			UUID laneId,

			@Valid
			@RequestBody
			UpdateLaneRequest request

	) {
		return toResponse(service.rename(boardId, laneId, accountId, request.name()));
	}

	@PutMapping("/order")
	List<LaneResponse> reorder(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@Valid
			@RequestBody
			ReorderLanesRequest request

	) {
		return service.reorder(boardId, accountId, request.laneIds()).stream().map(this::toResponse).toList();
	}

	@PostMapping("/{laneId}/archive")
	LaneResponse archive(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@PathVariable
			UUID laneId

	) {
		return toResponse(service.archive(boardId, laneId, accountId));
	}

	@PostMapping("/{laneId}/restore")
	LaneResponse restore(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@PathVariable
			UUID laneId

	) {
		return toResponse(service.restore(boardId, laneId, accountId));
	}

	@GetMapping
	List<LaneResponse> list(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@RequestParam(required = false)
			Boolean archived

	) {
		return service.list(boardId, accountId, archived).stream().map(this::toResponse).toList();
	}

	private LaneResponse toResponse(Lane lane) {
		return new LaneResponse(lane.getId(), lane.getName(), lane.getPosition(),
				lane.getCreatedAt(), lane.getUpdatedAt(), lane.getArchivedAt());
	}

}
