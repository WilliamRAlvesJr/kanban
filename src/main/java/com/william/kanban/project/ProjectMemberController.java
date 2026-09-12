package com.william.kanban.project;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/members")
class ProjectMemberController {

	private final ProjectMemberService service;

	private final ProjectMemberModelAssembler assembler;

	ProjectMemberController(ProjectMemberService service, ProjectMemberModelAssembler assembler) {
		this.service = service;
		this.assembler = assembler;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void add(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@Valid
			@RequestBody
			AddProjectMemberRequest request

	) {
		service.add(projectId, accountId, request.email());
	}

	@GetMapping
	CollectionModel<?> list(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId

	) {
		return assembler.toCollection(service.list(projectId, accountId), projectId);
	}

	@GetMapping("/{memberId}")
	EntityModel<ProjectMemberResponse> findById(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@PathVariable
			UUID memberId

	) {
		return assembler.toModel(service.findById(projectId, memberId, accountId));
	}

	@PutMapping("/{memberId}/permissions")
	RepresentationModel<?> updatePermissions(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@PathVariable
			UUID memberId,

			@Valid
			@RequestBody
			UpdateProjectMemberPermissionsRequest request

	) {
		return assembler.selfOf(service.updatePermissions(projectId, memberId, accountId, request.permissions()));
	}

	@DeleteMapping("/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void remove(

			@AuthenticationPrincipal
			UUID accountId,

			@PathVariable
			UUID projectId,

			@PathVariable
			UUID memberId

	) {
		service.remove(projectId, memberId, accountId);
	}

}
