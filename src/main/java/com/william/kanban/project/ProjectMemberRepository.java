package com.william.kanban.project;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

	@Modifying
	@Query(value = """
			insert into project_members (id, project_id, account_id, created_at)
			values (:id, :projectId, :accountId, :createdAt)
			on conflict (project_id, account_id) do nothing""", nativeQuery = true)
	void insertIgnoringDuplicate(UUID id, UUID projectId, UUID accountId, OffsetDateTime createdAt);

	@EntityGraph(attributePaths = "permissions")
	List<ProjectMember> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

	@EntityGraph(attributePaths = "permissions")
	Optional<ProjectMember> findByIdAndProjectId(UUID id, UUID projectId);

	@EntityGraph(attributePaths = "permissions")
	Optional<ProjectMember> findByProjectIdAndAccountId(UUID projectId, UUID accountId);

	@EntityGraph(attributePaths = "permissions")
	List<ProjectMember> findByAccountIdAndProjectIdIn(UUID accountId, Collection<UUID> projectIds);

}
