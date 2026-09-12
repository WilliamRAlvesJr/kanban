package com.william.kanban.board;

import com.william.kanban.project.ProjectAccess;
import jakarta.validation.Valid;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BoardController {

	/** GET /boards/{id} só atende a dona do projeto, então o quadro sai com os links dela. */
	private static final ProjectAccess OWNER_ACCESS = new ProjectAccess(true, Set.of());

	private final BoardService service;

	private final BoardModelAssembler assembler;

	BoardController(BoardService service, BoardModelAssembler assembler) {
		this.service = service;
		this.assembler = assembler;
	}

	@PostMapping("/projects/{projectId}/boards")
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RepresentationModel<?>> create(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@Valid
			@RequestBody
			CreateBoardRequest request

	) {
		RepresentationModel<?> model =
				assembler.selfOf(service.create(projectId, accountId, request.name(), request.description()));
		return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
	}

	@GetMapping("/projects/{projectId}/boards")
	CollectionModel<?> list(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@RequestParam(required = false)
			Boolean archived

	) {
		return assembler.toCollection(service.list(projectId, accountId, archived), projectId);
	}

	@GetMapping("/boards/{id}")
	EntityModel<BoardResponse> findById(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return assembler.toModel(service.findById(id, accountId), OWNER_ACCESS);
	}

	@PutMapping("/boards/{id}")
	RepresentationModel<?> update(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id,

			@Valid
			@RequestBody
			UpdateBoardRequest request

	) {
		return assembler.selfOf(service.update(id, accountId, request.name(), request.description()));
	}

	@PostMapping("/boards/{id}/archive")
	RepresentationModel<?> archive(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return assembler.selfOf(service.archive(id, accountId));
	}

	@PostMapping("/boards/{id}/restore")
	RepresentationModel<?> restore(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return assembler.selfOf(service.restore(id, accountId));
	}

	@PostMapping("/boards/{id}/move")
	RepresentationModel<?> move(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id,

			@Valid
			@RequestBody
			MoveBoardRequest request

	) {
		return assembler.selfOf(service.move(id, accountId, request.projectId()));
	}

}
