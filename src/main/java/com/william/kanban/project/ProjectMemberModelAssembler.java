package com.william.kanban.project;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.stereotype.Component;

@Component
class ProjectMemberModelAssembler {

	private static final EmbeddedWrappers WRAPPERS = new EmbeddedWrappers(false);

	EntityModel<ProjectMemberResponse> toModel(ProjectMemberView view) {
		String self = hrefOf(view.member());
		EntityModel<ProjectMemberResponse> model = EntityModel.of(toResponse(view), Link.of(self));
		if (view.access().allows(ProjectPermission.REMOVE_MEMBER)) {
			model.add(Link.of(self, "remove"));
		}
		if (view.access().allows(ProjectPermission.EDIT_MEMBER)) {
			model.add(Link.of(self + "/permissions", "edit-permissions"));
		}
		return model;
	}

	/** CollectionModel sem item omite _embedded; o wrapper vazio mantém o array. */
	CollectionModel<?> toCollection(ProjectMembers members, UUID projectId) {
		CollectionModel<?> collection = members.members().isEmpty()
				? CollectionModel.of(List.of(WRAPPERS.emptyCollectionOf(ProjectMemberResponse.class)))
				: CollectionModel.of(members.members().stream().map(this::toModel).toList());
		String project = "/projects/" + projectId;
		collection.add(Link.of(project + "/members"));
		if (members.access().allows(ProjectPermission.ADD_MEMBER)) {
			collection.add(Link.of(project + "/members", "add-member"));
		}
		if (members.access().allows(ProjectPermission.VIEW_PROJECT)) {
			collection.add(Link.of(project, "project"));
		}
		return collection;
	}

	RepresentationModel<?> selfOf(ProjectMember member) {
		return new RepresentationModel<>(Link.of(hrefOf(member)));
	}

	private static String hrefOf(ProjectMember member) {
		return "/projects/" + member.getProjectId() + "/members/" + member.getId();
	}

	private static ProjectMemberResponse toResponse(ProjectMemberView view) {
		ProjectMember member = view.member();
		Set<ProjectPermission> permissions = EnumSet.noneOf(ProjectPermission.class);
		permissions.addAll(member.getPermissions());
		return new ProjectMemberResponse(member.getId(), member.getAccountId(), view.account().email(),
				view.account().displayName(), permissions, member.getCreatedAt());
	}

}
