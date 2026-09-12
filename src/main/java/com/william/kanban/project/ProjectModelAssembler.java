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

	EntityModel<ProjectResponse> toModel(ProjectView view) {
		Project project = view.project();
		ProjectAccess access = view.access();
		String self = "/projects/" + project.getId();
		EntityModel<ProjectResponse> model = EntityModel.of(toResponse(project), Link.of(self));
		addIf(model, access.allows(ProjectPermission.EDIT_PROJECT), Link.of(self, "edit"));
		if (project.getArchivedAt() == null) {
			addIf(model, access.allows(ProjectPermission.ARCHIVE_PROJECT), Link.of(self + "/archive", "archive"));
		} else {
			addIf(model, access.allows(ProjectPermission.RESTORE_PROJECT), Link.of(self + "/restore", "restore"));
		}
		addIf(model, access.allows(ProjectPermission.VIEW_BOARDS), Link.of(self + "/boards", "boards"));
		addIf(model, access.allows(ProjectPermission.ADD_BOARDS), Link.of(self + "/boards", "create-board"));
		addIf(model, access.allows(ProjectPermission.VIEW_MEMBER), Link.of(self + "/members", "members"));
		addIf(model, access.allows(ProjectPermission.ADD_MEMBER), Link.of(self + "/members", "add-member"));
		return model;
	}

	/** CollectionModel sem item omite _embedded; o wrapper vazio mantém o array. */
	CollectionModel<?> toCollection(List<ProjectView> views) {
		CollectionModel<?> collection = views.isEmpty()
				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(ProjectResponse.class)))
				: CollectionModel.of(views.stream().map(this::toModel).toList());
		return collection.add(Link.of("/projects"), Link.of("/projects", "create-project"));
	}

	RepresentationModel<?> selfOf(Project project) {
		return new RepresentationModel<>(Link.of("/projects/" + project.getId()));
	}

	private static void addIf(RepresentationModel<?> model, boolean condition, Link link) {
		if (condition) {
			model.add(link);
		}
	}

	private static ProjectResponse toResponse(Project project) {
		return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
				project.getCreatedAt(), project.getUpdatedAt(), project.getArchivedAt());
	}

}
