package com.william.kanban.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.william.kanban.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.Map;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProjectApiTest {

	private static final String HAL_JSON = "application/hal+json";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void createsProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		String location = mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Produto", "description": "Time de produto"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader(HttpHeaders.LOCATION);

		mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$.id").value(idOf(location)))
				.andExpect(jsonPath("$.name").value("Produto"))
				.andExpect(jsonPath("$.description").value("Time de produto"))
				.andExpect(jsonPath("$.created_at").isNotEmpty())
				.andExpect(jsonPath("$.updated_at").isNotEmpty())
				.andExpect(jsonPath("$.archived_at").doesNotExist());
	}

	@Test
	void createsProjectWithoutDescription() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		String location = mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Produto"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader(HttpHeaders.LOCATION);

		mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$.name").value("Produto"))
				.andExpect(jsonPath("$.description").doesNotExist());
	}

	@Test
	void takesOwnerFromToken() throws Exception {
		String anaId = createAccount("ana@exemplo.com");
		String brunoId = createAccount("bruno@exemplo.com");

		String id = createProject(tokenOf("bruno@exemplo.com"), """
				{"name": "Produto"}
				""");

		assertThat(jdbcTemplate.queryForObject(
				"select owner_id from projects where id = ?", UUID.class, UUID.fromString(id)))
				.isEqualTo(UUID.fromString(brunoId))
				.isNotEqualTo(UUID.fromString(anaId));
	}

	@Test
	void createdProjectHasUpdatedAt() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		String location = mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Produto"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader(HttpHeaders.LOCATION);

		mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$.updated_at").isNotEmpty());
	}

	static Stream<String> invalidCreatePayloads() {
		return Stream.of(
				"""
						{"description": "Time de produto"}""",
				"""
						{"name": ""}""",
				"""
						{"name": "%s"}""".formatted("a".repeat(101)),
				"""
						{"name": "Produto", "description": "%s"}""".formatted("a".repeat(501)));
	}

	@ParameterizedTest
	@MethodSource("invalidCreatePayloads")
	void rejectsInvalidCreatePayload(String body) throws Exception {
		mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(countProjects()).isZero();
	}

	@Test
	void listsAllProjectsWithoutParameter() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String active = createProject(token, """
				{"name": "Ativo"}
				""");
		String archived = createProject(token, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		mockMvc.perform(get("/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.projects[*].id").value(containsInAnyOrder(active, archived)));
	}

	@Test
	void listsOnlyActiveProjects() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String active = createProject(token, """
				{"name": "Ativo"}
				""");
		String archived = createProject(token, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		mockMvc.perform(get("/projects").param("archived", "false")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.projects[*].id").value(contains(active)));
	}

	@Test
	void listsOnlyArchivedProjects() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		createProject(token, """
				{"name": "Ativo"}
				""");
		String archived = createProject(token, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		mockMvc.perform(get("/projects").param("archived", "true")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.projects[*].id").value(contains(archived)));
	}

	@Test
	void listsNewestProjectFirst() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String first = createProject(token, """
				{"name": "Primeiro"}
				""");
		String second = createProject(token, """
				{"name": "Segundo"}
				""");
		String third = createProject(token, """
				{"name": "Terceiro"}
				""");

		mockMvc.perform(get("/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.projects[*].id").value(contains(third, second, first)));
	}

	@Test
	void rejectsInvalidArchivedParameter() throws Exception {
		mockMvc.perform(get("/projects").param("archived", "talvez")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listsOnlyProjectsOfTokenOwner() throws Exception {
		String anaToken = tokenOf("ana@exemplo.com");
		String anaProject = createProject(anaToken, """
				{"name": "Da Ana"}
				""");
		createProject(tokenOf("bruno@exemplo.com"), """
				{"name": "Do Bruno"}
				""");

		mockMvc.perform(get("/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.projects[*].id").value(contains(anaProject)));
	}

	@Test
	void linksProjectList() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		createProject(token, """
				{"name": "Produto"}
				""");

		mockMvc.perform(get("/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$._links", aMapWithSize(2)))
				.andExpect(jsonPath("$._links.self.href").value("/projects"))
				.andExpect(jsonPath("$._links['create-project'].href").value("/projects"));
	}

	@Test
	void embedsSameProjectAsSingleRead() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto", "description": "Time de produto"}
				""");

		String list = mockMvc.perform(get("/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$._embedded.projects", hasSize(1)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String single = mockMvc.perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(JsonPath.<Map<String, Object>>read(list, "$._embedded.projects[0]"))
				.isEqualTo(JsonPath.<Map<String, Object>>read(single, "$"));
	}

	@Test
	void embedsEmptyProjectList() throws Exception {
		mockMvc.perform(get("/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.projects", hasSize(0)));
	}

	@Test
	void linksProjectListToPathWithoutQuery() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		archive(createProject(token, """
				{"name": "Produto"}
				"""));

		mockMvc.perform(get("/projects").param("archived", "true")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$._links.self.href").value("/projects"));
	}

	@Test
	void returnsActiveProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto", "description": "Time de produto"}
				""");

		mockMvc.perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.name").value("Produto"))
				.andExpect(jsonPath("$.description").value("Time de produto"))
				.andExpect(jsonPath("$.created_at").isNotEmpty())
				.andExpect(jsonPath("$.updated_at").isNotEmpty())
				.andExpect(jsonPath("$.archived_at").doesNotExist());
	}

	@Test
	void returnsArchivedProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		archive(id);

		mockMvc.perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.archived_at").isNotEmpty());
	}

	@Test
	void rendersProjectAsHal() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");

		mockMvc.perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, HAL_JSON))
				.andExpect(jsonPath("$._links.self.href").value("/projects/" + id));
	}

	@Test
	void rendersProjectAsHalWhenAccepted() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");

		mockMvc.perform(get("/projects/" + id).accept(HAL_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, HAL_JSON));
	}

	@Test
	void linksActiveProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");

		mockMvc.perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$._links", aMapWithSize(5)))
				.andExpect(jsonPath("$._links.self.href").value("/projects/" + id))
				.andExpect(jsonPath("$._links.edit.href").value("/projects/" + id))
				.andExpect(jsonPath("$._links.archive.href").value("/projects/" + id + "/archive"))
				.andExpect(jsonPath("$._links.boards.href").value("/projects/" + id + "/boards"))
				.andExpect(jsonPath("$._links['create-board'].href").value("/projects/" + id + "/boards"));
	}

	@Test
	void linksArchivedProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		archive(id);

		mockMvc.perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$._links", aMapWithSize(5)))
				.andExpect(jsonPath("$._links.self.href").value("/projects/" + id))
				.andExpect(jsonPath("$._links.edit.href").value("/projects/" + id))
				.andExpect(jsonPath("$._links.restore.href").value("/projects/" + id + "/restore"))
				.andExpect(jsonPath("$._links.boards.href").value("/projects/" + id + "/boards"))
				.andExpect(jsonPath("$._links['create-board'].href").value("/projects/" + id + "/boards"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "ARCHIVE"})
	void hidesActiveProjectOfAnotherOwner(String request) throws Exception {
		String id = createProject(tokenOf("bruno@exemplo.com"), """
				{"name": "Produto"}
				""");
		String anaToken = tokenOf("ana@exemplo.com");
		String unknownId = UUID.randomUUID().toString();

		String foreign = mockMvc.perform(requestOf(request, id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = mockMvc.perform(requestOf(request, unknownId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(id, unknownId)).isEqualTo(unknown);
		assertThat(nameOf(id)).isEqualTo("Produto");
		assertThat(archivedAtOf(id)).isNull();
	}

	@Test
	void returnsNotFoundForUnknownProject() throws Exception {
		mockMvc.perform(get("/projects/" + UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com")))
				.andExpect(status().isNotFound());
	}

	@Test
	void answersErrorAsProblemDetailWithoutLinks() throws Exception {
		mockMvc.perform(get("/projects/" + UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com")))
				.andExpect(status().isNotFound())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.andExpect(jsonPath("$._links").doesNotExist());
	}

	@Test
	void replacesProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto", "description": "Time de produto"}
				""");

		putProject(token, id, """
				{"name": "Plataforma", "description": "Outro texto"}
				""")
				.andExpect(status().isOk());

		getProject(token, id)
				.andExpect(jsonPath("$.name").value("Plataforma"))
				.andExpect(jsonPath("$.description").value("Outro texto"));
	}

	@Test
	void erasesOmittedDescription() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto", "description": "Time de produto"}
				""");

		putProject(token, id, """
				{"name": "Produto"}
				""")
				.andExpect(status().isOk());

		getProject(token, id).andExpect(jsonPath("$.description").doesNotExist());
	}

	@Test
	void keepsArchivedAtOnReplace() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		archive(id);
		OffsetDateTime archivedAt = archivedAtOf(id);

		putProject(token, id, """
				{"name": "Plataforma"}
				""")
				.andExpect(status().isOk());

		getProject(token, id).andExpect(jsonPath("$.name").value("Plataforma"));
		assertThat(archivedAtOf(id)).isEqualTo(archivedAt);
	}

	static Stream<String> invalidReplacePayloads() {
		return Stream.of(
				"""
						{"description": "Time de produto"}""",
				"""
						{"name": "", "description": "Time de produto"}""",
				"""
						{"name": "%s", "description": "Time de produto"}""".formatted("a".repeat(101)),
				"""
						{"name": "Produto", "description": "%s"}""".formatted("a".repeat(501)));
	}

	@ParameterizedTest
	@MethodSource("invalidReplacePayloads")
	void rejectsInvalidReplacePayload(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto", "description": "Time de produto"}
				""");

		putProject(token, id, body).andExpect(status().isBadRequest());

		assertThat(nameOf(id)).isEqualTo("Produto");
		assertThat(descriptionOf(id)).isEqualTo("Time de produto");
	}

	@Test
	void stampsUpdatedAtOnChange() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		putProject(token, id, """
				{"name": "Plataforma"}
				""")
				.andExpect(status().isOk());

		assertThat(updatedAtOf(id)).isAfter(updatedAt);
	}

	@Test
	void keepsUpdatedAtWhenNothingChanges() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto", "description": "Time de produto"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		putProject(token, id, """
				{"name": "Produto", "description": "Time de produto"}
				""")
				.andExpect(status().isOk());

		assertThat(updatedAtOf(id)).isEqualTo(updatedAt);
	}

	@Test
	void archivesProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");

		postAction(token, id, "archive").andExpect(status().isOk());

		getProject(token, id).andExpect(jsonPath("$.archived_at").isNotEmpty());

		mockMvc.perform(get("/projects").param("archived", "false")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$._embedded.projects[*].id").value(not(hasItem(id))));
	}

	@Test
	void restoresProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		archive(id);

		postAction(token, id, "restore").andExpect(status().isOk());

		getProject(token, id).andExpect(jsonPath("$.archived_at").doesNotExist());

		mockMvc.perform(get("/projects").param("archived", "false")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$._embedded.projects[*].id").value(hasItem(id)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"PUT", "ARCHIVE", "RESTORE"})
	void respondsToProjectWriteWithSelfOnly(String request) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");

		mockMvc.perform(requestOf(request, id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$._links", aMapWithSize(1)))
				.andExpect(jsonPath("$._links.self.href").value("/projects/" + id));
	}

	@Test
	void respondsToProjectCreationWithSelfOnly() throws Exception {
		String response = mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Plataforma"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$._links", aMapWithSize(1)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		UUID created = jdbcTemplate.queryForObject("select id from projects", UUID.class);

		assertThat(JsonPath.<String>read(response, "$._links.self.href")).isEqualTo("/projects/" + created);
	}

	@Test
	void pointsLocationOfProjectCreationToSelf() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("ana@exemplo.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Plataforma"}
								"""))
				.andReturn()
				.getResponse();

		assertThat(response.getHeader(HttpHeaders.LOCATION))
				.isEqualTo(JsonPath.<String>read(response.getContentAsString(), "$._links.self.href"));
	}

	@Test
	void keepsProjectUnchangedOnRepeatedArchive() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		archive(id);
		OffsetDateTime archivedAt = archivedAtOf(id);
		OffsetDateTime updatedAt = updatedAtOf(id);

		postAction(token, id, "archive").andExpect(status().isOk());

		assertThat(archivedAtOf(id)).isEqualTo(archivedAt);
		assertThat(updatedAtOf(id)).isEqualTo(updatedAt);
	}

	@Test
	void restoresActiveProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		OffsetDateTime updatedAt = updatedAtOf(id);

		postAction(token, id, "restore").andExpect(status().isOk());

		getProject(token, id).andExpect(jsonPath("$.archived_at").doesNotExist());
		assertThat(updatedAtOf(id)).isEqualTo(updatedAt);
	}

	@Test
	void doesNotRestoreProjectOfAnotherOwner() throws Exception {
		String id = createProject(tokenOf("bruno@exemplo.com"), """
				{"name": "Produto"}
				""");
		archive(id);

		postAction(tokenOf("ana@exemplo.com"), id, "restore")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Projeto não encontrado: " + id));

		assertThat(archivedAtOf(id)).isNotNull();
	}

	@Test
	void keepsBoardsOnProjectArchive() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		String active = createBoard(token, id);
		String archived = createBoard(token, id);
		jdbcTemplate.update("update boards set archived_at = now() where id = ?", UUID.fromString(archived));
		Map<String, Object> activeBefore = boardTimestampsOf(active);
		Map<String, Object> archivedBefore = boardTimestampsOf(archived);

		postAction(token, id, "archive").andExpect(status().isOk());

		assertThat(boardTimestampsOf(active)).isEqualTo(activeBefore);
		assertThat(boardTimestampsOf(archived)).isEqualTo(archivedBefore);
	}

	@Test
	void keepsBoardsOnProjectRestore() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String id = createProject(token, """
				{"name": "Produto"}
				""");
		String board = createBoard(token, id);
		jdbcTemplate.update("update boards set archived_at = now() where id = ?", UUID.fromString(board));
		archive(id);
		Map<String, Object> before = boardTimestampsOf(board);

		postAction(token, id, "restore").andExpect(status().isOk());

		assertThat(boardTimestampsOf(board)).isEqualTo(before);
	}

	@Test
	void deletesProjectsWithAccount() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		createProject(token, """
				{"name": "Ativo"}
				""");
		String archived = createProject(token, """
				{"name": "Arquivado"}
				""");
		archive(archived);

		jdbcTemplate.update("delete from accounts where email = ?", "ana@exemplo.com");

		assertThat(countProjects()).isZero();
	}

	@Test
	void keepsProjectsOfAnotherAccountWhenDeletingAccount() throws Exception {
		createProject(tokenOf("ana@exemplo.com"), """
				{"name": "Da Ana"}
				""");
		String brunoProject = createProject(tokenOf("bruno@exemplo.com"), """
				{"name": "Do Bruno"}
				""");

		jdbcTemplate.update("delete from accounts where email = ?", "ana@exemplo.com");

		assertThat(jdbcTemplate.queryForList("select id from projects", UUID.class))
				.containsExactly(UUID.fromString(brunoProject));
	}

	@Test
	void documentsProjectEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects'].post.responses.201").exists())
				.andExpect(jsonPath("$.paths['/projects'].post.responses.400").exists())
				.andExpect(jsonPath("$.paths['/projects'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects'].post.responses.404").doesNotExist())
				.andExpect(jsonPath("$.paths['/projects'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects'].get.responses.400").exists())
				.andExpect(jsonPath("$.paths['/projects'].get.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}'].get.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}'].get.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}'].put.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}'].put.responses.400").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}'].put.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}'].put.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/archive'].post.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/archive'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/archive'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/restore'].post.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/restore'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/restore'].post.responses.404").exists())
				.andExpect(jsonPath("$.components.schemas.EntityModelProjectResponse.properties.archived_at").exists())
				.andExpect(jsonPath("$.components.schemas.UpdateProjectRequest.properties.name").exists());
	}

	private String createBoard(String token, String projectId) throws Exception {
		String location = mockMvc.perform(post("/projects/" + projectId + "/boards")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Sprint 12"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader(HttpHeaders.LOCATION);
		return idOf(location);
	}

	private Map<String, Object> boardTimestampsOf(String boardId) {
		return jdbcTemplate.queryForMap(
				"select archived_at, updated_at from boards where id = ?", UUID.fromString(boardId));
	}

	private ResultActions postAction(String token, String id, String action) throws Exception {
		return mockMvc.perform(post("/projects/" + id + "/" + action)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions putProject(String token, String id, String body) throws Exception {
		return mockMvc.perform(put("/projects/" + id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private MockHttpServletRequestBuilder requestOf(String request, String id) {
		return switch (request) {
			case "GET" -> get("/projects/" + id);
			case "PUT" -> put("/projects/" + id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"name": "Outro"}
							""");
			case "ARCHIVE" -> post("/projects/" + id + "/archive");
			case "RESTORE" -> post("/projects/" + id + "/restore");
			default -> throw new IllegalArgumentException(request);
		};
	}

	private String createProject(String token, String body) throws Exception {
		return idOf(mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader(HttpHeaders.LOCATION));
	}

	private ResultActions getProject(String token, String id) throws Exception {
		return mockMvc.perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private static String idOf(String location) {
		return location.substring(location.lastIndexOf('/') + 1);
	}

	private String createAccount(String email) throws Exception {
		mockMvc.perform(post("/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "display_name": "Ana", "password": "segredo"}
								""".formatted(email)))
				.andExpect(status().isCreated());
		return jdbcTemplate.queryForObject("select id from accounts where email = ?", UUID.class, email).toString();
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
		jdbcTemplate.update("update projects set archived_at = now() where id = ?", UUID.fromString(id));
	}

	private String nameOf(String id) {
		return jdbcTemplate.queryForObject(
				"select name from projects where id = ?", String.class, UUID.fromString(id));
	}

	private String descriptionOf(String id) {
		return jdbcTemplate.queryForObject(
				"select description from projects where id = ?", String.class, UUID.fromString(id));
	}

	private OffsetDateTime updatedAtOf(String id) {
		return jdbcTemplate.queryForObject(
				"select updated_at from projects where id = ?", OffsetDateTime.class, UUID.fromString(id));
	}

	private OffsetDateTime archivedAtOf(String id) {
		return jdbcTemplate.queryForObject(
				"select archived_at from projects where id = ?", OffsetDateTime.class, UUID.fromString(id));
	}

	private Integer countProjects() {
		return jdbcTemplate.queryForObject("select count(*) from projects", Integer.class);
	}

	private Integer countAccounts(String email) {
		return jdbcTemplate.queryForObject(
				"select count(*) from accounts where email = ?", Integer.class, email);
	}

}
