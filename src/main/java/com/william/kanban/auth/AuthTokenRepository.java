package com.william.kanban.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

	Optional<AuthToken> findByTokenHash(String tokenHash);

	void deleteByTokenHash(String tokenHash);

}
