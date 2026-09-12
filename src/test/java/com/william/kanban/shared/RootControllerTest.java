package com.william.kanban.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.security.core.context.SecurityContextHolder;

class RootControllerTest {

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void linksAnonymousEntryPointWithoutAuthentication() {
		SecurityContextHolder.clearContext();

		assertThat(new RootController().root().getLinks())
				.extracting(Link::getRel)
				.extracting(LinkRelation::value)
				.containsExactly("self", "login", "create-account");
	}

}
