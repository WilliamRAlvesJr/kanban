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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/boards")
class BoardController {

	private final BoardService service;

	BoardController(BoardService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	BoardResponse create(

			@AuthenticationPrincipal
			UUID accountId,

			@Valid
			@RequestBody
			CreateBoardRequest request

	) {
		return toResponse(service.create(accountId, request.name(), request.description()));
	}

	@GetMapping
	List<BoardResponse> list(

			@AuthenticationPrincipal
			UUID accountId,

			@RequestParam(required = false)
			Boolean archived

	) {
		return service.list(accountId, archived).stream().map(this::toResponse).toList();
	}

	@GetMapping("/{id}")
	BoardResponse findById(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return toResponse(service.findById(id, accountId));
	}

	@PutMapping("/{id}")
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

	@PostMapping("/{id}/archive")
	BoardResponse archive(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return toResponse(service.archive(id, accountId));
	}

	@PostMapping("/{id}/restore")
	BoardResponse restore(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID id

	) {
		return toResponse(service.restore(id, accountId));
	}

	private BoardResponse toResponse(Board board) {
		return new BoardResponse(board.getId(), board.getName(), board.getDescription(),
				board.getCreatedAt(), board.getUpdatedAt(), board.getArchivedAt());
	}

}
