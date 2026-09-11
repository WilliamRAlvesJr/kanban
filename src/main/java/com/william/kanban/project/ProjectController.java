package com.william.kanban.project;

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
@RequestMapping("/projects")
class ProjectController {

	private final ProjectService service;

	ProjectController(ProjectService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ProjectResponse create(

			@AuthenticationPrincipal
			UUID accountId,

			@Valid
			@RequestBody
			CreateProjectRequest request

	) {
		return toResponse(service.create(accountId, request.name(), request.description()));
	}

	@GetMapping
	List<ProjectResponse> list(

			@AuthenticationPrincipal
			UUID accountId,

			@RequestParam(required = false)
			Boolean archived

	) {
		return service.list(accountId, archived).stream().map(this::toResponse).toList();
	}

	@GetMapping("/{projectId}")
	ProjectResponse findById(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId

	) {
		return toResponse(service.findById(projectId, accountId));
	}

	@PutMapping("/{projectId}")
	ProjectResponse update(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@Valid
			@RequestBody
			UpdateProjectRequest request

	) {
		return toResponse(service.update(projectId, accountId, request.name(), request.description()));
	}

	@PostMapping("/{projectId}/archive")
	ProjectResponse archive(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId

	) {
		return toResponse(service.archive(projectId, accountId));
	}

	@PostMapping("/{projectId}/restore")
	ProjectResponse restore(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId

	) {
		return toResponse(service.restore(projectId, accountId));
	}

	private ProjectResponse toResponse(Project project) {
		return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
				project.getCreatedAt(), project.getUpdatedAt(), project.getArchivedAt());
	}

}
