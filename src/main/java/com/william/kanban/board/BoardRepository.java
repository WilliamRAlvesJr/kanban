package com.william.kanban.board;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface BoardRepository extends JpaRepository<Board, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Board> findWithLockById(UUID id);

	List<Board> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

	List<Board> findByProjectIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID projectId);

	List<Board> findByProjectIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID projectId);

}
