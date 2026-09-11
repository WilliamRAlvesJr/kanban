package com.william.kanban.project;

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
@RequestMapping("/projects")
class ProjectController {

	private final ProjectService service;

	private final ProjectModelAssembler assembler;

	ProjectController(ProjectService service, ProjectModelAssembler assembler) {
		this.service = service;
		this.assembler = assembler;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RepresentationModel<?>> create(

			@AuthenticationPrincipal
			UUID accountId,

			@Valid
			@RequestBody
			CreateProjectRequest request

	) {
		RepresentationModel<?> model = assembler.selfOf(service.create(accountId, request.name(), request.description()));
		return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
	}

	@GetMapping
	CollectionModel<?> list(

			@AuthenticationPrincipal
			UUID accountId,

			@RequestParam(required = false)
			Boolean archived

	) {
		return assembler.toCollection(service.list(accountId, archived));
	}

	@GetMapping("/{projectId}")
	EntityModel<ProjectResponse> findById(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId

	) {
		return assembler.toModel(service.findById(projectId, accountId));
	}

	@PutMapping("/{projectId}")
	RepresentationModel<?> update(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@Valid
			@RequestBody
			UpdateProjectRequest request

	) {
		return assembler.selfOf(service.update(projectId, accountId, request.name(), request.description()));
	}

	@PostMapping("/{projectId}/archive")
	RepresentationModel<?> archive(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId

	) {
		return assembler.selfOf(service.archive(projectId, accountId));
	}

	@PostMapping("/{projectId}/restore")
	RepresentationModel<?> restore(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId

	) {
		return assembler.selfOf(service.restore(projectId, accountId));
	}

}
