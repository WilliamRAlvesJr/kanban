package com.william.kanban.board;

import com.william.kanban.project.ProjectAccess;
import com.william.kanban.project.ProjectPermission;
import com.william.kanban.project.ProjectService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

	private final BoardRepository repository;

	private final ProjectService projectService;

	BoardService(BoardRepository repository, ProjectService projectService) {
		this.repository = repository;
		this.projectService = projectService;
	}

	Board create(UUID projectId, UUID accountId, String name, String description) {
		projectService.requireAccess(projectId, accountId, ProjectPermission.ADD_BOARDS);
		return repository.save(new Board(projectId, name, description));
	}

	public void requireOwned(UUID id, UUID ownerId) {
		findById(id, ownerId);
	}

	/** Trava a linha do quadro até o fim da transação de quem chama. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockOwned(UUID id, UUID ownerId) {
		repository.findWithLockById(id)
				.filter(board -> projectService.isOwned(board.getProjectId(), ownerId))
				.orElseThrow(() -> new BoardNotFoundException(id));
	}

	Board findById(UUID id, UUID ownerId) {
		return repository.findById(id)
				.filter(board -> projectService.isOwned(board.getProjectId(), ownerId))
				.orElseThrow(() -> new BoardNotFoundException(id));
	}

	@Transactional
	Board update(UUID id, UUID ownerId, String name, String description) {
		Board board = findById(id, ownerId);
		board.setName(name);
		board.setDescription(description);
		return board;
	}

	@Transactional
	Board archive(UUID id, UUID ownerId) {
		Board board = findById(id, ownerId);
		if (board.getArchivedAt() == null) {
			board.setArchivedAt(OffsetDateTime.now());
		}
		return board;
	}

	@Transactional
	Board restore(UUID id, UUID ownerId) {
		Board board = findById(id, ownerId);
		board.setArchivedAt(null);
		return board;
	}

	@Transactional
	Board move(UUID id, UUID ownerId, UUID projectId) {
		Board board = findById(id, ownerId);
		projectService.requireOwned(projectId, ownerId);
		board.setProjectId(projectId);
		return board;
	}

	ProjectBoards list(UUID projectId, UUID accountId, Boolean archived) {
		ProjectAccess access = projectService.requireAccess(projectId, accountId, ProjectPermission.VIEW_BOARDS);
		return new ProjectBoards(boardsOf(projectId, archived), access);
	}

	private List<Board> boardsOf(UUID projectId, Boolean archived) {
		if (archived == null) {
			return repository.findByProjectIdOrderByCreatedAtDesc(projectId);
		}
		return archived
				? repository.findByProjectIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(projectId)
				: repository.findByProjectIdAndArchivedAtIsNullOrderByCreatedAtDesc(projectId);
	}

}
