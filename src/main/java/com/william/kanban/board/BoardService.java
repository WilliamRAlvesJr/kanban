package com.william.kanban.board;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

	private final BoardRepository repository;

	BoardService(BoardRepository repository) {
		this.repository = repository;
	}

	Board create(UUID ownerId, String name, String description) {
		return repository.save(new Board(ownerId, name, description));
	}

	public void requireOwned(UUID id, UUID ownerId) {
		findById(id, ownerId);
	}

	/** Trava a linha do quadro até o fim da transação de quem chama. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockOwned(UUID id, UUID ownerId) {
		repository.findWithLockByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new BoardNotFoundException(id));
	}

	Board findById(UUID id, UUID ownerId) {
		return repository.findByIdAndOwnerId(id, ownerId)
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

	List<Board> list(UUID ownerId, Boolean archived) {
		if (archived == null) {
			return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
		}
		return archived
				? repository.findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(ownerId)
				: repository.findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(ownerId);
	}

}
