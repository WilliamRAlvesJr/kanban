package com.william.kanban.lane;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	private final LaneModelAssembler assembler;

	LaneController(LaneService service, LaneModelAssembler assembler) {
		this.service = service;
		this.assembler = assembler;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RepresentationModel<?>> create(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@Valid
			@RequestBody
			CreateLaneRequest request

	) {
		RepresentationModel<?> model = assembler.selfOf(service.create(boardId, accountId, request.name()));
		return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
	}

	@GetMapping("/{laneId}")
	EntityModel<LaneResponse> findById(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@PathVariable
			UUID laneId

	) {
		return assembler.toModel(service.findById(boardId, laneId, accountId));
	}

	@PutMapping("/{laneId}")
	RepresentationModel<?> rename(

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
		return assembler.selfOf(service.rename(boardId, laneId, accountId, request.name()));
	}

	@PutMapping("/order")
	RepresentationModel<?> reorder(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@Valid
			@RequestBody
			ReorderLanesRequest request

	) {
		service.reorder(boardId, accountId, request.laneIds());
		return assembler.selfOfCollection(boardId);
	}

	@PostMapping("/{laneId}/archive")
	RepresentationModel<?> archive(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@PathVariable
			UUID laneId

	) {
		return assembler.selfOf(service.archive(boardId, laneId, accountId));
	}

	@PostMapping("/{laneId}/restore")
	RepresentationModel<?> restore(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@PathVariable
			UUID laneId

	) {
		return assembler.selfOf(service.restore(boardId, laneId, accountId));
	}

	@GetMapping
	CollectionModel<?> list(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID boardId,

			@RequestParam(required = false)
			Boolean archived

	) {
		return assembler.toCollection(service.list(boardId, accountId, archived), boardId);
	}

}
