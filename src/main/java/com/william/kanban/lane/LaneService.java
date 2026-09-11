package com.william.kanban.lane;

import static java.util.stream.Collectors.toSet;

import com.william.kanban.board.BoardService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LaneService {

	private final LaneRepository repository;

	private final BoardService boardService;

	LaneService(LaneRepository repository, BoardService boardService) {
		this.repository = repository;
		this.boardService = boardService;
	}

	@Transactional
	Lane create(UUID boardId, UUID ownerId, String name) {
		boardService.lockOwned(boardId, ownerId);
		int position = (int) repository.countByBoardIdAndArchivedAtIsNull(boardId);
		return repository.save(new Lane(boardId, name, position));
	}

	@Transactional
	Lane rename(UUID boardId, UUID laneId, UUID ownerId, String name) {
		boardService.lockOwned(boardId, ownerId);
		Lane lane = findInBoard(boardId, laneId);
		lane.setName(name);
		return lane;
	}

	/** O CHECK de lanes confere cada comando, então archived_at e position saem no mesmo UPDATE. */
	@Transactional
	Lane archive(UUID boardId, UUID laneId, UUID ownerId) {
		boardService.lockOwned(boardId, ownerId);
		Lane lane = findInBoard(boardId, laneId);
		if (lane.getArchivedAt() != null) {
			return lane;
		}
		int position = lane.getPosition();
		lane.setArchivedAt(OffsetDateTime.now());
		lane.setPosition(null);
		repository.shiftDownAbove(boardId, position);
		return lane;
	}

	@Transactional
	Lane restore(UUID boardId, UUID laneId, UUID ownerId) {
		boardService.lockOwned(boardId, ownerId);
		Lane lane = findInBoard(boardId, laneId);
		if (lane.getArchivedAt() == null) {
			return lane;
		}
		lane.setPosition((int) repository.countByBoardIdAndArchivedAtIsNull(boardId));
		lane.setArchivedAt(null);
		return lane;
	}

	@Transactional
	List<Lane> reorder(UUID boardId, UUID ownerId, List<UUID> laneIds) {
		boardService.lockOwned(boardId, ownerId);
		if (!Set.copyOf(laneIds).equals(activeIdsOf(boardId))) {
			throw new LaneOrderMismatchException();
		}
		repository.parkActivePositions(boardId);
		for (int position = 0; position < laneIds.size(); position++) {
			repository.assignPosition(laneIds.get(position), position);
		}
		return repository.findByBoardIdAndArchivedAtIsNullOrderByPositionAsc(boardId);
	}

	List<Lane> list(UUID boardId, UUID ownerId, Boolean archived) {
		boardService.requireOwned(boardId, ownerId);
		if (archived == null) {
			return repository.findByBoardIdOrderByPositionAscArchivedAtDesc(boardId);
		}
		return archived
				? repository.findByBoardIdAndArchivedAtIsNotNullOrderByArchivedAtDesc(boardId)
				: repository.findByBoardIdAndArchivedAtIsNullOrderByPositionAsc(boardId);
	}

	private Set<UUID> activeIdsOf(UUID boardId) {
		return repository.findByBoardIdAndArchivedAtIsNullOrderByPositionAsc(boardId).stream()
				.map(Lane::getId)
				.collect(toSet());
	}

	private Lane findInBoard(UUID boardId, UUID laneId) {
		return repository.findByIdAndBoardId(laneId, boardId)
				.orElseThrow(() -> new LaneNotFoundException(laneId));
	}

}
