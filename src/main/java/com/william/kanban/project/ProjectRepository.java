package com.william.kanban.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProjectRepository extends JpaRepository<Project, UUID> {

	Optional<Project> findByIdAndOwnerId(UUID id, UUID ownerId);

	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

	List<Project> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<Project> findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID ownerId);

	List<Project> findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID ownerId);

}
