package com.william.kanban.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BoardRepository extends JpaRepository<Board, UUID> {

	Optional<Board> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<Board> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<Board> findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID ownerId);

	List<Board> findByOwnerIdAndArchivedAtIsNotNullOrderByCreatedAtDesc(UUID ownerId);

}
