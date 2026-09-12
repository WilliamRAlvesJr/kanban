package com.william.kanban.project;

import com.william.kanban.account.AccountService;
import com.william.kanban.account.AccountSummary;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProjectMemberService {

	private final ProjectMemberRepository repository;

	private final ProjectService projectService;

	private final AccountService accountService;

	ProjectMemberService(ProjectMemberRepository repository, ProjectService projectService,
			AccountService accountService) {
		this.repository = repository;
		this.projectService = projectService;
		this.accountService = accountService;
	}

	@Transactional
	void add(UUID projectId, UUID accountId, String email) {
		projectService.requireAccess(projectId, accountId, ProjectPermission.ADD_MEMBER);
		accountService.findIdByEmail(email)
				.filter(memberAccountId -> !projectService.isOwned(projectId, memberAccountId))
				.ifPresent(memberAccountId -> repository.insertIgnoringDuplicate(
						UUID.randomUUID(), projectId, memberAccountId, OffsetDateTime.now()));
	}

	ProjectMembers list(UUID projectId, UUID accountId) {
		ProjectAccess access = projectService.requireAccess(projectId, accountId, ProjectPermission.VIEW_MEMBER);
		List<ProjectMember> members = repository.findByProjectIdOrderByCreatedAtDesc(projectId);
		Map<UUID, AccountSummary> accounts =
				accountService.summariesOf(members.stream().map(ProjectMember::getAccountId).toList());
		return new ProjectMembers(members.stream()
				.map(member -> new ProjectMemberView(member, accounts.get(member.getAccountId()), access))
				.toList(), access);
	}

	ProjectMemberView findById(UUID projectId, UUID memberId, UUID accountId) {
		ProjectAccess access = projectService.requireAccess(projectId, accountId, ProjectPermission.VIEW_MEMBER);
		ProjectMember member = findInProject(projectId, memberId);
		UUID memberAccountId = member.getAccountId();
		return new ProjectMemberView(member, accountService.summariesOf(List.of(memberAccountId)).get(memberAccountId),
				access);
	}

	@Transactional
	ProjectMember updatePermissions(UUID projectId, UUID memberId, UUID accountId,
			Collection<ProjectPermission> permissions) {
		projectService.requireAccess(projectId, accountId, ProjectPermission.EDIT_MEMBER);
		ProjectMember member = findInProject(projectId, memberId);
		member.getPermissions().clear();
		member.getPermissions().addAll(permissions);
		return member;
	}

	@Transactional
	void remove(UUID projectId, UUID memberId, UUID accountId) {
		projectService.requireAccess(projectId, accountId, ProjectPermission.REMOVE_MEMBER);
		repository.delete(findInProject(projectId, memberId));
	}

	private ProjectMember findInProject(UUID projectId, UUID memberId) {
		return repository.findByIdAndProjectId(memberId, projectId)
				.orElseThrow(() -> new ProjectMemberNotFoundException(memberId));
	}

}
