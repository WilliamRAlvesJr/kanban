package com.william.kanban.project;

import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.stereotype.Component;

@Component
class ProjectModelAssembler {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

	EntityModel<ProjectResponse> toModel(Project project) {
		String self = "/projects/" + project.getId();
		EntityModel<ProjectResponse> model = EntityModel.of(toResponse(project), Link.of(self), Link.of(self, "edit"));
		model.add(project.getArchivedAt() == null
				? Link.of(self + "/archive", "archive")
				: Link.of(self + "/restore", "restore"));
		return model.add(Link.of(self + "/boards", "boards"), Link.of(self + "/boards", "create-board"));
	}

	/** CollectionModel sem item omite _embedded; o wrapper vazio mantém o array. */
	CollectionModel<?> toCollection(List<Project> projects) {
		CollectionModel<?> collection = projects.isEmpty()
				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(ProjectResponse.class)))
				: CollectionModel.of(projects.stream().map(this::toModel).toList());
		return collection.add(Link.of("/projects"), Link.of("/projects", "create-project"));
	}

	RepresentationModel<?> selfOf(Project project) {
		return new RepresentationModel<>(Link.of("/projects/" + project.getId()));
	}

	private static ProjectResponse toResponse(Project project) {
		return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
				project.getCreatedAt(), project.getUpdatedAt(), project.getArchivedAt());
	}

}
