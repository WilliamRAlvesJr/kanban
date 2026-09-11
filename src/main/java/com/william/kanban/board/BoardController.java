package com.william.kanban.board;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BoardController {

	private final BoardService service;

	BoardController(BoardService service) {
		this.service = service;
	}

	@PostMapping("/projects/{projectId}/boards")
	@ResponseStatus(HttpStatus.CREATED)
	BoardResponse create(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@Valid
			@RequestBody
			CreateBoardRequest request

	) {
		return toResponse(service.create(projectId, accountId, request.name(), request.description()));
	}

	@GetMapping("/projects/{projectId}/boards")
	List<BoardResponse> list(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@RequestParam(required = false)
			Boolean archived

	) {
		return service.list(projectId, accountId, archived).stream().map(this::toResponse).toList();
	}

	@GetMapping("/boards/{id}")
	BoardResponse findById(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return toResponse(service.findById(id, accountId));
	}

	@PutMapping("/boards/{id}")
	BoardResponse update(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id,

			@Valid
			@RequestBody
			UpdateBoardRequest request

	) {
		return toResponse(service.update(id, accountId, request.name(), request.description()));
	}

	@PostMapping("/boards/{id}/archive")
	BoardResponse archive(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return toResponse(service.archive(id, accountId));
	}

	@PostMapping("/boards/{id}/restore")
	BoardResponse restore(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return toResponse(service.restore(id, accountId));
	}

	@PostMapping("/boards/{id}/move")
	BoardResponse move(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id,

			@Valid
			@RequestBody
			MoveBoardRequest request

	) {
		return toResponse(service.move(id, accountId, request.projectId()));
	}

	private BoardResponse toResponse(Board board) {
		return new BoardResponse(board.getId(), board.getProjectId(), board.getName(), board.getDescription(),
				board.getCreatedAt(), board.getUpdatedAt(), board.getArchivedAt());
	}

}
