package com.william.kanban.project;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ProjectRepository extends JpaRepository<Project, UUID> {

	String VISIBLE_TO_ACCOUNT = """
			select p from Project p
			where (p.ownerId = :accountId
			       or exists (select 1 from ProjectMember m join m.permissions granted
			                  where m.projectId = p.id
			                    and m.accountId = :accountId
			                    and granted = com.william.kanban.project.ProjectPermission.VIEW_PROJECT))
			""";

	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

	@Query(VISIBLE_TO_ACCOUNT + "order by p.createdAt desc")
	List<Project> findVisibleOrderByCreatedAtDesc(UUID accountId);

	@Query(VISIBLE_TO_ACCOUNT + "and p.archivedAt is null order by p.createdAt desc")
	List<Project> findVisibleAndActiveOrderByCreatedAtDesc(UUID accountId);

	@Query(VISIBLE_TO_ACCOUNT + "and p.archivedAt is not null order by p.createdAt desc")
	List<Project> findVisibleAndArchivedOrderByCreatedAtDesc(UUID accountId);

}
