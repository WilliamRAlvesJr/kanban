package com.william.kanban.lane;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface LaneRepository extends JpaRepository<Lane, UUID> {

	long countByBoardIdAndArchivedAtIsNull(UUID boardId);

	Optional<Lane> findByIdAndBoardId(UUID id, UUID boardId);

	/** Query nativa não passa pelo @UpdateTimestamp: a lane deslocada fica com updated_at intacto. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE lanes SET position = position - 1 WHERE board_id = :boardId AND position > :position",
			nativeQuery = true)
	void shiftDownAbove(UUID boardId, int position);

	/** Posições negativas distintas: cada assignPosition seguinte grava um índice que ninguém ocupa. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE lanes SET position = -position - 1 WHERE board_id = :boardId AND archived_at IS NULL",
			nativeQuery = true)
	void parkActivePositions(UUID boardId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE lanes SET position = :position WHERE id = :id", nativeQuery = true)
	void assignPosition(UUID id, int position);

	/** O Postgres põe NULL por último em ASC: as arquivadas, sem position, vêm depois das ativas. */
	List<Lane> findByBoardIdOrderByPositionAscArchivedAtDesc(UUID boardId);

	List<Lane> findByBoardIdAndArchivedAtIsNullOrderByPositionAsc(UUID boardId);

	List<Lane> findByBoardIdAndArchivedAtIsNotNullOrderByArchivedAtDesc(UUID boardId);

}
