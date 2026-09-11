package com.william.kanban.board;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface BoardRepository extends JpaRepository<Board, UUID> {

	Optional<Board> findByIdAndOwnerId(UUID id, UUID ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Board> findWithLockByIdAndOwnerId(UUID id, UUID ownerId);

	List<Board> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<Board> findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID ownerId);

	List<Board> findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID ownerId);

}
