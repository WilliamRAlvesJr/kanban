package com.william.kanban.repository;

import com.william.kanban.entity.AuthToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

	Optional<AuthToken> findByTokenHash(String tokenHash);

	void deleteByTokenHash(String tokenHash);

}
