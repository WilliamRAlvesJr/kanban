package com.william.kanban.project;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

	private static final ProjectAccess OWNER_ACCESS = new ProjectAccess(true, Set.of());

	private final ProjectRepository repository;

	private final ProjectMemberRepository memberRepository;

	ProjectService(ProjectRepository repository, ProjectMemberRepository memberRepository) {
		this.repository = repository;
		this.memberRepository = memberRepository;
	}

	Project create(UUID ownerId, String name, String description) {
		return repository.save(new Project(ownerId, name, description));
	}

	public boolean isOwned(UUID id, UUID ownerId) {
		return repository.existsByIdAndOwnerId(id, ownerId);
	}

	public void requireOwned(UUID id, UUID ownerId) {
		if (!isOwned(id, ownerId)) {
			throw new ProjectNotFoundException(id);
		}
	}

	public ProjectAccess requireAccess(UUID id, UUID accountId, ProjectPermission permission) {
		return load(id, accountId, permission).access();
	}

	ProjectView findById(UUID id, UUID accountId) {
		return load(id, accountId, ProjectPermission.VIEW_PROJECT);
	}

	@Transactional
	Project update(UUID id, UUID accountId, String name, String description) {
		Project project = load(id, accountId, ProjectPermission.EDIT_PROJECT).project();
		project.setName(name);
		project.setDescription(description);
		return project;
	}

	@Transactional
	Project archive(UUID id, UUID accountId) {
		Project project = load(id, accountId, ProjectPermission.ARCHIVE_PROJECT).project();
		if (project.getArchivedAt() == null) {
			project.setArchivedAt(OffsetDateTime.now());
		}
		return project;
	}

	@Transactional
	Project restore(UUID id, UUID accountId) {
		Project project = load(id, accountId, ProjectPermission.RESTORE_PROJECT).project();
		project.setArchivedAt(null);
		return project;
	}

	List<ProjectView> list(UUID accountId, Boolean archived) {
		List<Project> projects = visibleTo(accountId, archived);
		Map<UUID, ProjectMember> members = memberRepository
				.findByAccountIdAndProjectIdIn(accountId, projects.stream().map(Project::getId).toList())
				.stream()
				.collect(toMap(ProjectMember::getProjectId, identity()));
		return projects.stream()
				.map(project -> new ProjectView(project, project.getOwnerId().equals(accountId)
						? OWNER_ACCESS
						: memberAccessOf(members.get(project.getId()))))
				.toList();
	}

	private List<Project> visibleTo(UUID accountId, Boolean archived) {
		if (archived == null) {
			return repository.findVisibleOrderByCreatedAtDesc(accountId);
		}
		return archived
				? repository.findVisibleAndArchivedOrderByCreatedAtDesc(accountId)
				: repository.findVisibleAndActiveOrderByCreatedAtDesc(accountId);
	}

	private ProjectView load(UUID id, UUID accountId, ProjectPermission permission) {
		Project project = repository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
		ProjectAccess access = accessOf(project, accountId);
		if (!access.allows(permission)) {
			throw new ProjectAccessDeniedException(id, permission);
		}
		return new ProjectView(project, access);
	}

	private ProjectAccess accessOf(Project project, UUID accountId) {
		if (project.getOwnerId().equals(accountId)) {
			return OWNER_ACCESS;
		}
		return memberRepository.findByProjectIdAndAccountId(project.getId(), accountId)
				.map(ProjectService::memberAccessOf)
				.orElseThrow(() -> new ProjectNotFoundException(project.getId()));
	}

	private static ProjectAccess memberAccessOf(ProjectMember member) {
		return new ProjectAccess(false, Set.copyOf(member.getPermissions()));
	}

}
