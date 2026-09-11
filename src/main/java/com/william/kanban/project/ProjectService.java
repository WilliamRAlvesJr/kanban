package com.william.kanban.project;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

	private final ProjectRepository repository;

	ProjectService(ProjectRepository repository) {
		this.repository = repository;
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

	Project findById(UUID id, UUID ownerId) {
		return repository.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new ProjectNotFoundException(id));
	}

	@Transactional
	Project update(UUID id, UUID ownerId, String name, String description) {
		Project project = findById(id, ownerId);
		project.setName(name);
		project.setDescription(description);
		return project;
	}

	@Transactional
	Project archive(UUID id, UUID ownerId) {
		Project project = findById(id, ownerId);
		if (project.getArchivedAt() == null) {
			project.setArchivedAt(OffsetDateTime.now());
		}
		return project;
	}

	@Transactional
	Project restore(UUID id, UUID ownerId) {
		Project project = findById(id, ownerId);
		project.setArchivedAt(null);
		return project;
	}

	List<Project> list(UUID ownerId, Boolean archived) {
		if (archived == null) {
			return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
		}
		return archived
				? repository.findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(ownerId)
				: repository.findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(ownerId);
	}

}
