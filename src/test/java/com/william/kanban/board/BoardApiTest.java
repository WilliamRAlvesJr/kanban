package com.william.kanban.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.william.kanban.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BoardApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void createsBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);

		postBoard(token, projectId, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.project_id").value(projectId))
				.andExpect(jsonPath("$.name").value("Sprint 12"))
				.andExpect(jsonPath("$.description").value("Trabalho da sprint"))
				.andExpect(jsonPath("$.created_at").isNotEmpty())
				.andExpect(jsonPath("$.updated_at").isNotEmpty())
				.andExpect(jsonPath("$.archived_at").doesNotExist());
	}

	@Test
	void createsBoardWithoutDescription() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		postBoard(token, createProject(token), """
				{"name": "Sprint 12"}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Sprint 12"))
				.andExpect(jsonPath("$.description").doesNotExist());
	}

	@Test
	void createsBoardInProjectOfUrl() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		createProject(token);
		String secondProjectId = createProject(token);

		String id = createBoard(token, secondProjectId, """
				{"name": "Sprint 12"}
				""");

		assertThat(projectIdOf(id)).isEqualTo(UUID.fromString(secondProjectId));
	}

	@Test
	void createsBoardInArchivedProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		archiveProject(projectId);

		postBoard(token, projectId, """
				{"name": "Sprint 12"}
				""")
				.andExpect(status().isCreated());

		assertThat(jdbcTemplate.queryForObject("select archived_at from projects where id = ?",
				OffsetDateTime.class, UUID.fromString(projectId))).isNotNull();
	}

	@Test
	void createdBoardHasUpdatedAt() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		postBoard(token, createProject(token), """
				{"name": "Sprint 12"}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.updated_at").isNotEmpty());
	}

	@Test
	void doesNotCreateBoardInProjectOfAnotherOwner() throws Exception {
		String projectId = createProject(tokenOf("bruno@exemplo.com"));
		String anaToken = tokenOf("ana@exemplo.com");
		String unknownId = UUID.randomUUID().toString();
		String body = """
				{"name": "Sprint 12"}
				""";

		String foreign = postBoard(anaToken, projectId, body)
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = postBoard(anaToken, unknownId, body)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Projeto não encontrado: " + unknownId))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(projectId, unknownId)).isEqualTo(unknown);
		assertThat(countBoards()).isZero();
	}

	@Test
	void returnsNotFoundForUnknownProjectOnCreate() throws Exception {
		postBoard(tokenOf("ana@exemplo.com"), UUID.randomUUID().toString(), """
				{"name": "Sprint 12"}
				""")
				.andExpect(status().isNotFound());
	}

	static Stream<String> invalidCreatePayloads() {
		return Stream.of(
				"""
						{"description": "Trabalho da sprint"}""",
				"""
						{"name": ""}""",
				"""
						{"name": "%s"}""".formatted("a".repeat(101)),
				"""
						{"name": "Sprint 12", "description": "%s"}""".formatted("a".repeat(501)));
	}

	@ParameterizedTest
	@MethodSource("invalidCreatePayloads")
	void rejectsInvalidCreatePayload(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");

		postBoard(token, createProject(token), body).andExpect(status().isBadRequest());

		assertThat(countBoards()).isZero();
	}

	@Test
	void returnsActiveBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""");

		mockMvc.perform(get("/boards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.project_id").value(projectIdOf(id).toString()))
				.andExpect(jsonPath("$.name").value("Sprint 12"))
				.andExpect(jsonPath("$.description").value("Trabalho da sprint"))
				.andExpect(jsonPath("$.created_at").isNotEmpty())
				.andExpect(jsonPath("$.updated_at").isNotEmpty())
				.andExpect(jsonPath("$.archived_at").doesNotExist());
	}

	@Test
	void returnsArchivedBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		archive(id);

		mockMvc.perform(get("/boards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.archived_at").isNotEmpty());
	}

	@Test
	void hidesBoardOfAnotherOwner() throws Exception {
		String id = createBoard(tokenOf("bruno@exemplo.com"), """
				{"name": "Sprint 12"}
				""");
		String anaToken = tokenOf("ana@exemplo.com");
		UUID unknownId = UUID.randomUUID();

		String foreign = mockMvc.perform(
						get("/boards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = mockMvc.perform(
						get("/boards/" + unknownId).header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(id, unknownId.toString())).isEqualTo(unknown);
	}

	@Test
	void returnsNotFoundForUnknownBoard() throws Exception {
		mockMvc.perform(get("/boards/" + UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com")))
				.andExpect(status().isNotFound());
	}

	@Test
	void listsAllBoardsWithoutParameter() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String active = createBoard(token, projectId, """
				{"name": "Ativo"}
				""");
		String archived = createBoard(token, projectId, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		getBoards(token, projectId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id").value(containsInAnyOrder(active, archived)));
	}

	@Test
	void listsOnlyActiveBoards() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String active = createBoard(token, projectId, """
				{"name": "Ativo"}
				""");
		String archived = createBoard(token, projectId, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		getBoards(token, projectId, "false")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id").value(contains(active)));
	}

	@Test
	void listsOnlyArchivedBoards() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		createBoard(token, projectId, """
				{"name": "Ativo"}
				""");
		String archived = createBoard(token, projectId, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		getBoards(token, projectId, "true")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id").value(contains(archived)));
	}

	@Test
	void listsNewestBoardFirst() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String first = createBoard(token, projectId, """
				{"name": "Primeiro"}
				""");
		String second = createBoard(token, projectId, """
				{"name": "Segundo"}
				""");
		String third = createBoard(token, projectId, """
				{"name": "Terceiro"}
				""");

		getBoards(token, projectId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id").value(contains(third, second, first)));
	}

	@Test
	void rejectsInvalidArchivedParameter() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		getBoards(token, createProject(token), "talvez").andExpect(status().isBadRequest());
	}

	@Test
	void listsOnlyBoardsOfProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String firstProjectId = createProject(token);
		String firstBoard = createBoard(token, firstProjectId, """
				{"name": "Do primeiro"}
				""");
		createBoard(token, createProject(token), """
				{"name": "Do segundo"}
				""");

		getBoards(token, firstProjectId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id").value(contains(firstBoard)));
	}

	@Test
	void doesNotListBoardsOfProjectOfAnotherOwner() throws Exception {
		String brunoToken = tokenOf("bruno@exemplo.com");
		String projectId = createProject(brunoToken);
		createBoard(brunoToken, projectId, """
				{"name": "Do Bruno"}
				""");
		String anaToken = tokenOf("ana@exemplo.com");
		String unknownId = UUID.randomUUID().toString();

		String foreign = getBoards(anaToken, projectId)
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = getBoards(anaToken, unknownId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Projeto não encontrado: " + unknownId))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(projectId, unknownId)).isEqualTo(unknown);
	}

	@Test
	void movesBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String produto = createProject(token);
		String operacoes = createProject(token);
		String id = createBoard(token, produto, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""");
		createLane(token, id, "A fazer");
		createLane(token, id, "Feito");
		archive(id);
		OffsetDateTime archivedAt = archivedAtOf(id);

		moveBoard(token, id, projectBody(operacoes))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.project_id").value(operacoes));

		getBoards(token, operacoes).andExpect(jsonPath("$[*].id").value(contains(id)));
		getBoards(token, produto).andExpect(jsonPath("$[*].id").value(not(hasItem(id))));
		assertThat(jdbcTemplate.queryForList(
				"select name from lanes where board_id = ? order by position", String.class, UUID.fromString(id)))
				.containsExactly("A fazer", "Feito");
		assertThat(nameOf(id)).isEqualTo("Sprint 12");
		assertThat(descriptionOf(id)).isEqualTo("Trabalho da sprint");
		assertThat(archivedAtOf(id)).isEqualTo(archivedAt);
	}

	@Test
	void movesBoardToArchivedProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		String archivedProjectId = createProject(token);
		archiveProject(archivedProjectId);

		moveBoard(token, id, projectBody(archivedProjectId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.project_id").value(archivedProjectId));
	}

	@Test
	void keepsBoardUnchangedOnMoveToCurrentProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String id = createBoard(token, projectId, """
				{"name": "Sprint 12"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		moveBoard(token, id, projectBody(projectId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.project_id").value(projectId));

		assertThat(updatedAtOf(id)).isEqualTo(updatedAt);
	}

	@Test
	void doesNotMoveBoardToProjectOfAnotherOwner() throws Exception {
		String anaToken = tokenOf("ana@exemplo.com");
		String produto = createProject(anaToken);
		String id = createBoard(anaToken, produto, """
				{"name": "Sprint 12"}
				""");
		String brunoProjectId = createProject(tokenOf("bruno@exemplo.com"));
		String unknownId = UUID.randomUUID().toString();

		String foreign = moveBoard(anaToken, id, projectBody(brunoProjectId))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = moveBoard(anaToken, id, projectBody(unknownId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Projeto não encontrado: " + unknownId))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(brunoProjectId, unknownId)).isEqualTo(unknown);
		assertThat(projectIdOf(id)).isEqualTo(UUID.fromString(produto));
	}

	@Test
	void doesNotMoveBoardToUnknownProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String produto = createProject(token);
		String id = createBoard(token, produto, """
				{"name": "Sprint 12"}
				""");

		moveBoard(token, id, projectBody(UUID.randomUUID().toString()))
				.andExpect(status().isNotFound());

		assertThat(projectIdOf(id)).isEqualTo(UUID.fromString(produto));
	}

	@Test
	void doesNotMoveBoardOfAnotherOwner() throws Exception {
		String brunoToken = tokenOf("bruno@exemplo.com");
		String brunoProjectId = createProject(brunoToken);
		String id = createBoard(brunoToken, brunoProjectId, """
				{"name": "Sprint 12"}
				""");
		String anaToken = tokenOf("ana@exemplo.com");
		String anaProjectId = createProject(anaToken);
		String unknownId = UUID.randomUUID().toString();

		String foreign = moveBoard(anaToken, id, projectBody(anaProjectId))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = moveBoard(anaToken, unknownId, projectBody(anaProjectId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Quadro não encontrado: " + unknownId))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(id, unknownId)).isEqualTo(unknown);
		assertThat(projectIdOf(id)).isEqualTo(UUID.fromString(brunoProjectId));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"project_id\": null}", "{\"project_id\": \"abc\"}"})
	void rejectsInvalidMovePayload(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String produto = createProject(token);
		String id = createBoard(token, produto, """
				{"name": "Sprint 12"}
				""");

		moveBoard(token, id, body).andExpect(status().isBadRequest());

		assertThat(projectIdOf(id)).isEqualTo(UUID.fromString(produto));
	}

	@Test
	void stampsUpdatedAtOnMove() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		moveBoard(token, id, projectBody(createProject(token))).andExpect(status().isOk());

		assertThat(updatedAtOf(id)).isAfter(updatedAt);
	}

	@Test
	void replacesBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""");

		putBoard(token, id, """
				{"name": "Sprint 13", "description": "Outro texto"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Sprint 13"))
				.andExpect(jsonPath("$.description").value("Outro texto"));

		assertThat(nameOf(id)).isEqualTo("Sprint 13");
	}

	@Test
	void erasesOmittedDescription() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""");

		putBoard(token, id, """
				{"name": "Sprint 12"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").doesNotExist());
	}

	@Test
	void keepsArchivedAtOnReplace() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		archive(id);
		OffsetDateTime archivedAt = archivedAtOf(id);

		putBoard(token, id, """
				{"name": "Sprint 13"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Sprint 13"));

		assertThat(archivedAtOf(id)).isEqualTo(archivedAt);
	}

	@Test
	void doesNotReplaceBoardOfAnotherOwner() throws Exception {
		String id = createBoard(tokenOf("bruno@exemplo.com"), """
				{"name": "Sprint 12"}
				""");

		putBoard(tokenOf("ana@exemplo.com"), id, """
				{"name": "Sprint 13"}
				""")
				.andExpect(status().isNotFound());

		assertThat(nameOf(id)).isEqualTo("Sprint 12");
	}

	static Stream<String> invalidReplacePayloads() {
		return Stream.of(
				"""
						{"description": "Trabalho da sprint"}""",
				"""
						{"name": "", "description": "Trabalho da sprint"}""",
				"""
						{"name": "%s", "description": "Trabalho da sprint"}""".formatted("a".repeat(101)),
				"""
						{"name": "Sprint 12", "description": "%s"}""".formatted("a".repeat(501)));
	}

	@ParameterizedTest
	@MethodSource("invalidReplacePayloads")
	void rejectsInvalidReplacePayload(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""");

		putBoard(token, id, body).andExpect(status().isBadRequest());

		assertThat(nameOf(id)).isEqualTo("Sprint 12");
		assertThat(descriptionOf(id)).isEqualTo("Trabalho da sprint");
	}

	@Test
	void stampsUpdatedAtOnChange() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		putBoard(token, id, """
				{"name": "Sprint 13"}
				""")
				.andExpect(status().isOk());

		assertThat(updatedAtOf(id)).isAfter(updatedAt);
	}

	@Test
	void keepsUpdatedAtWhenNothingChanges() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		putBoard(token, id, """
				{"name": "Sprint 12", "description": "Trabalho da sprint"}
				""")
				.andExpect(status().isOk());

		assertThat(updatedAtOf(id)).isEqualTo(updatedAt);
	}

	@Test
	void archivesBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");

		postAction(token, id, "archive")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archived_at").isNotEmpty());

		getBoards(token, projectIdOf(id).toString(), "false")
				.andExpect(jsonPath("$[*].id").value(not(hasItem(id))));
	}

	@Test
	void restoresBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		archive(id);

		postAction(token, id, "restore")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.archived_at").doesNotExist());

		getBoards(token, projectIdOf(id).toString(), "false")
				.andExpect(jsonPath("$[*].id").value(hasItem(id)));
	}

	@Test
	void keepsBoardUnchangedOnRepeatedArchive() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		archive(id);
		OffsetDateTime archivedAt = archivedAtOf(id);
		OffsetDateTime updatedAt = updatedAtOf(id);

		postAction(token, id, "archive").andExpect(status().isOk());

		assertThat(archivedAtOf(id)).isEqualTo(archivedAt);
		assertThat(updatedAtOf(id)).isEqualTo(updatedAt);
	}

	@Test
	void restoresActiveBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createBoard(token, """
				{"name": "Sprint 12"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		postAction(token, id, "restore")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.archived_at").doesNotExist());

		assertThat(updatedAtOf(id)).isEqualTo(updatedAt);
	}

	@Test
	void doesNotArchiveBoardOfAnotherOwner() throws Exception {
		String id = createBoard(tokenOf("bruno@exemplo.com"), """
				{"name": "Sprint 12"}
				""");

		postAction(tokenOf("ana@exemplo.com"), id, "archive")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Quadro não encontrado: " + id));

		assertThat(archivedAtOf(id)).isNull();
	}

	@Test
	void doesNotRestoreBoardOfAnotherOwner() throws Exception {
		String id = createBoard(tokenOf("bruno@exemplo.com"), """
				{"name": "Sprint 12"}
				""");
		archive(id);

		postAction(tokenOf("ana@exemplo.com"), id, "restore")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Quadro não encontrado: " + id));

		assertThat(archivedAtOf(id)).isNotNull();
	}

	@Test
	void deletesBoardsWithAccount() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		createBoard(token, """
				{"name": "Ativo"}
				""");
		String archived = createBoard(token, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		jdbcTemplate.update("delete from accounts where email = ?", "ana@exemplo.com");

		assertThat(countBoards()).isZero();
	}

	@Test
	void keepsBoardsOfAnotherAccountWhenDeletingAccount() throws Exception {
		createBoard(tokenOf("ana@exemplo.com"), """
				{"name": "Da Ana"}
				""");
		String brunoBoard = createBoard(tokenOf("bruno@exemplo.com"), """
				{"name": "Do Bruno"}
				""");

		jdbcTemplate.update("delete from accounts where email = ?", "ana@exemplo.com");

		assertThat(jdbcTemplate.queryForList("select id from boards", UUID.class))
				.containsExactly(UUID.fromString(brunoBoard));
	}

	@Test
	void deletesBoardsWithProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		createBoard(token, projectId, """
				{"name": "Ativo"}
				""");
		String archived = createBoard(token, projectId, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		jdbcTemplate.update("delete from projects where id = ?", UUID.fromString(projectId));

		assertThat(countBoards()).isZero();
	}

	@Test
	void keepsBoardsOfAnotherProjectWhenDeletingProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String firstProjectId = createProject(token);
		createBoard(token, firstProjectId, """
				{"name": "Do primeiro"}
				""");
		String secondBoard = createBoard(token, """
				{"name": "Do segundo"}
				""");

		jdbcTemplate.update("delete from projects where id = ?", UUID.fromString(firstProjectId));

		assertThat(jdbcTemplate.queryForList("select id from boards", UUID.class))
				.containsExactly(UUID.fromString(secondBoard));
	}

	@Test
	void documentsBoardEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].post.responses.201").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].post.responses.400").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].post.responses.200").doesNotExist())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].get.responses.400").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].get.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/boards'].get.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/move'].post.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/move'].post.responses.400").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/move'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/move'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards'].get").doesNotExist())
				.andExpect(jsonPath("$.paths['/boards'].post").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.BoardResponse.properties.project_id").exists())
				.andExpect(jsonPath("$.components.schemas.MoveBoardRequest.properties.project_id").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}'].get.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}'].get.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}'].put.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}'].put.responses.400").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}'].put.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}'].put.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/archive'].post.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/archive'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/archive'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/restore'].post.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/restore'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{id}/restore'].post.responses.404").exists())
				.andExpect(jsonPath("$.components.schemas.BoardResponse.properties.archived_at").exists())
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.detail").exists())
				.andExpect(jsonPath("$.components.schemas.UpdateBoardRequest.properties.name").exists());
	}

	private ResultActions postAction(String token, String id, String action) throws Exception {
		return mockMvc.perform(post("/boards/" + id + "/" + action)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions putBoard(String token, String id, String body) throws Exception {
		return mockMvc.perform(put("/boards/" + id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions moveBoard(String token, String id, String body) throws Exception {
		return mockMvc.perform(post("/boards/" + id + "/move")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private static String projectBody(String projectId) {
		return """
				{"project_id": "%s"}
				""".formatted(projectId);
	}

	private void createLane(String token, String boardId, String name) throws Exception {
		mockMvc.perform(post("/boards/" + boardId + "/lanes")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "%s"}
								""".formatted(name)))
				.andExpect(status().isCreated());
	}

	private ResultActions getBoards(String token, String projectId) throws Exception {
		return mockMvc.perform(get("/projects/" + projectId + "/boards")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions getBoards(String token, String projectId, String archived) throws Exception {
		return mockMvc.perform(get("/projects/" + projectId + "/boards").param("archived", archived)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions postBoard(String token, String projectId, String body) throws Exception {
		return mockMvc.perform(post("/projects/" + projectId + "/boards")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String createBoard(String token, String body) throws Exception {
		return createBoard(token, createProject(token), body);
	}

	private String createBoard(String token, String projectId, String body) throws Exception {
		String response = postBoard(token, projectId, body)
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private String createProject(String token) throws Exception {
		String response = mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Produto"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private String createAccount(String email) throws Exception {
		String body = mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "display_name": "Ana", "password": "segredo"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private String tokenOf(String email) throws Exception {
		if (countAccounts(email) == 0) {
			createAccount(email);
		}
		String body = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "password": "segredo"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private void archive(String id) {
		jdbcTemplate.update("update boards set archived_at = now() where id = ?", UUID.fromString(id));
	}

	private void archiveProject(String projectId) {
		jdbcTemplate.update("update projects set archived_at = now() where id = ?", UUID.fromString(projectId));
	}

	private UUID projectIdOf(String id) {
		return jdbcTemplate.queryForObject(
				"select project_id from boards where id = ?", UUID.class, UUID.fromString(id));
	}

	private String nameOf(String id) {
		return jdbcTemplate.queryForObject(
				"select name from boards where id = ?", String.class, UUID.fromString(id));
	}

	private String descriptionOf(String id) {
		return jdbcTemplate.queryForObject(
				"select description from boards where id = ?", String.class, UUID.fromString(id));
	}

	private OffsetDateTime updatedAtOf(String id) {
		return jdbcTemplate.queryForObject(
				"select updated_at from boards where id = ?", OffsetDateTime.class, UUID.fromString(id));
	}

	private OffsetDateTime archivedAtOf(String id) {
		return jdbcTemplate.queryForObject(
				"select archived_at from boards where id = ?", OffsetDateTime.class, UUID.fromString(id));
	}

	private Integer countBoards() {
		return jdbcTemplate.queryForObject("select count(*) from boards", Integer.class);
	}

	private Integer countAccounts(String email) {
		return jdbcTemplate.queryForObject(
				"select count(*) from accounts where email = ?", Integer.class, email);
	}

}
