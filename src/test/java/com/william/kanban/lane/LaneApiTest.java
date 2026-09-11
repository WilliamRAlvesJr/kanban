package com.william.kanban.lane;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Named.named;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.william.kanban.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LaneApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void createsLaneInEmptyBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);

		postLane(token, boardId, "A fazer")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.name").value("A fazer"))
				.andExpect(jsonPath("$.position").value(0))
				.andExpect(jsonPath("$.created_at").isNotEmpty())
				.andExpect(jsonPath("$.updated_at").isNotEmpty())
				.andExpect(jsonPath("$.archived_at").value(nullValue()));
	}

	@Test
	void createsLaneAtTheEnd() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "A fazer");
		createLane(token, boardId, "Fazendo");
		archive(createLane(token, boardId, "Descartadas"));

		postLane(token, boardId, "Feito")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.position").value(2));
	}

	@Test
	void acceptsRepeatedNameInBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "A fazer");

		postLane(token, boardId, "A fazer").andExpect(status().isCreated());

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from lanes where board_id = ? and name = ?",
				Integer.class, UUID.fromString(boardId), "A fazer"))
				.isEqualTo(2);
	}

	@Test
	void keepsPositionSequenceUnderConcurrentCreates() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> statuses;

		try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
			statuses = IntStream.range(0, 10)
					.mapToObj(i -> executor.submit(() -> {
						start.await();
						return postLane(token, boardId, "Lane " + i).andReturn().getResponse().getStatus();
					}))
					.toList();
			start.countDown();
		}

		for (Future<Integer> status : statuses) {
			assertThat(status.get()).isEqualTo(201);
		}
		assertThat(activePositionsOf(boardId)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
	}

	static Stream<String> invalidNamePayloads() {
		return Stream.of(
				"{}",
				"""
						{"name": ""}""",
				"""
						{"name": "%s"}""".formatted("a".repeat(101)));
	}

	@ParameterizedTest
	@MethodSource("invalidNamePayloads")
	void rejectsInvalidNameOnCreate(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);

		mockMvc.perform(post(lanesOf(boardId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(countLanes()).isZero();
	}

	@Test
	void listsActiveByPositionThenArchivedByNewest() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "Feito");
		createLane(token, boardId, "A fazer");
		jdbcTemplate.update("update lanes set position = 1 - position where board_id = ?", UUID.fromString(boardId));
		archive(createLane(token, boardId, "Antigas"));
		archive(createLane(token, boardId, "Descartadas"));

		getLanes(token, boardId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name").value(contains("A fazer", "Feito", "Descartadas", "Antigas")));
	}

	@Test
	void listsOnlyActiveLanes() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "A fazer");
		createLane(token, boardId, "Feito");
		archive(createLane(token, boardId, "Descartadas"));

		getLanes(token, boardId, "false")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name").value(contains("A fazer", "Feito")));
	}

	@Test
	void listsOnlyArchivedLanesByNewest() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "A fazer");
		archive(createLane(token, boardId, "Antigas"));
		archive(createLane(token, boardId, "Descartadas"));

		getLanes(token, boardId, "true")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name").value(contains("Descartadas", "Antigas")));
	}

	@Test
	void rejectsInvalidArchivedParameter() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);

		getLanes(token, boardId, "talvez").andExpect(status().isBadRequest());
	}

	@Test
	void listsOnlyLanesOfRequestedBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String firstBoardId = createBoard(token);
		String secondBoardId = createBoard(token);
		createLane(token, firstBoardId, "A fazer");
		createLane(token, secondBoardId, "Backlog");

		getLanes(token, firstBoardId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name").value(contains("A fazer")));
	}

	@Test
	void returnsNotFoundForUnknownBoard() throws Exception {
		getLanes(tokenOf("ana@exemplo.com"), UUID.randomUUID().toString()).andExpect(status().isNotFound());
	}

	@Test
	void replacesName() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		createLane(token, boardId, "Feito");

		putLane(token, boardId, aFazerId, "Backlog")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Backlog"))
				.andExpect(jsonPath("$.position").value(0));

		assertThat(nameOf(aFazerId)).isEqualTo("Backlog");
	}

	@Test
	void renamesArchivedLane() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String laneId = createLane(token, boardId, "Descartadas");
		archive(laneId);
		OffsetDateTime archivedAt = archivedAtOf(laneId);

		putLane(token, boardId, laneId, "Canceladas")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Canceladas"))
				.andExpect(jsonPath("$.position").value(nullValue()));

		assertThat(archivedAtOf(laneId)).isEqualTo(archivedAt);
	}

	@ParameterizedTest
	@MethodSource("invalidNamePayloads")
	void rejectsInvalidNameOnReplace(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String laneId = createLane(token, boardId, "A fazer");

		mockMvc.perform(put(laneOf(boardId, laneId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(nameOf(laneId)).isEqualTo("A fazer");
	}

	@Test
	void stampsUpdatedAtOnRename() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String laneId = createLane(token, boardId, "A fazer");
		OffsetDateTime updatedAt = updatedAtOf(laneId);

		putLane(token, boardId, laneId, "Backlog").andExpect(status().isOk());

		assertThat(updatedAtOf(laneId)).isAfter(updatedAt);
	}

	@Test
	void keepsUpdatedAtWhenNameDoesNotChange() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String laneId = createLane(token, boardId, "A fazer");
		OffsetDateTime updatedAt = updatedAtOf(laneId);

		putLane(token, boardId, laneId, "A fazer").andExpect(status().isOk());

		assertThat(updatedAtOf(laneId)).isEqualTo(updatedAt);
	}

	@Test
	void archivesLane() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String fazendoId = createLane(token, boardId, "Fazendo");
		String feitoId = createLane(token, boardId, "Feito");

		postAction(token, boardId, fazendoId, "archive")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archived_at").isNotEmpty())
				.andExpect(jsonPath("$.position").value(nullValue()));

		assertThat(positionOf(aFazerId)).isZero();
		assertThat(positionOf(feitoId)).isEqualTo(1);
	}

	@Test
	void restoresLaneAtTheEnd() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String feitoId = createLane(token, boardId, "Feito");
		String fazendoId = createLane(token, boardId, "Fazendo");
		archive(fazendoId);

		postAction(token, boardId, fazendoId, "restore")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archived_at").value(nullValue()))
				.andExpect(jsonPath("$.position").value(2));

		assertThat(positionOf(aFazerId)).isZero();
		assertThat(positionOf(feitoId)).isEqualTo(1);
	}

	@Test
	void keepsLaneUnchangedOnRepeatedArchive() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String fazendoId = createLane(token, boardId, "Fazendo");
		archive(fazendoId);
		OffsetDateTime archivedAt = archivedAtOf(fazendoId);

		postAction(token, boardId, fazendoId, "archive")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.position").value(nullValue()));

		assertThat(archivedAtOf(fazendoId)).isEqualTo(archivedAt);
		assertThat(positionOf(aFazerId)).isZero();
	}

	@Test
	void restoresActiveLane() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String feitoId = createLane(token, boardId, "Feito");

		postAction(token, boardId, aFazerId, "restore")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archived_at").value(nullValue()))
				.andExpect(jsonPath("$.position").value(0));

		assertThat(positionOf(feitoId)).isEqualTo(1);
	}

	@Test
	void stampsUpdatedAtOnArchive() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String laneId = createLane(token, boardId, "A fazer");
		OffsetDateTime updatedAt = updatedAtOf(laneId);

		postAction(token, boardId, laneId, "archive").andExpect(status().isOk());

		assertThat(updatedAtOf(laneId)).isAfter(updatedAt);
	}

	@Test
	void stampsUpdatedAtOnRestore() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String laneId = createLane(token, boardId, "A fazer");
		archive(laneId);
		OffsetDateTime updatedAt = updatedAtOf(laneId);

		postAction(token, boardId, laneId, "restore").andExpect(status().isOk());

		assertThat(updatedAtOf(laneId)).isAfter(updatedAt);
	}

	@Test
	void keepsUpdatedAtOnRepeatedArchive() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String laneId = createLane(token, boardId, "A fazer");
		archive(laneId);
		OffsetDateTime updatedAt = updatedAtOf(laneId);

		postAction(token, boardId, laneId, "archive").andExpect(status().isOk());

		assertThat(updatedAtOf(laneId)).isEqualTo(updatedAt);
	}

	@Test
	void keepsUpdatedAtOfLaneShiftedByArchive() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String feitoId = createLane(token, boardId, "Feito");
		OffsetDateTime updatedAt = updatedAtOf(feitoId);

		postAction(token, boardId, aFazerId, "archive").andExpect(status().isOk());

		assertThat(positionOf(feitoId)).isZero();
		assertThat(updatedAtOf(feitoId)).isEqualTo(updatedAt);
	}

	@RepeatedTest(20)
	void keepsPositionSequenceUnderConcurrentArchiveAndCreate() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "A fazer");
		String fazendoId = createLane(token, boardId, "Fazendo");
		createLane(token, boardId, "Feito");
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> statuses;

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			statuses = Stream.<Callable<Integer>>of(
							() -> {
								start.await();
								return postAction(token, boardId, fazendoId, "archive")
										.andReturn().getResponse().getStatus();
							},
							() -> {
								start.await();
								return postLane(token, boardId, "Revisão").andReturn().getResponse().getStatus();
							})
					.map(executor::submit)
					.toList();
			start.countDown();
		}

		assertThat(statuses.get(0).get()).isEqualTo(200);
		assertThat(statuses.get(1).get()).isEqualTo(201);
		assertThat(activePositionsOf(boardId)).containsExactly(0, 1, 2);
	}

	@Test
	void rewritesOrderOfActiveLanes() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String fazendoId = createLane(token, boardId, "Fazendo");
		String feitoId = createLane(token, boardId, "Feito");

		putOrder(token, boardId, feitoId, aFazerId, fazendoId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name").value(contains("Feito", "A fazer", "Fazendo")))
				.andExpect(jsonPath("$[*].position").value(contains(0, 1, 2)));

		assertThat(positionOf(feitoId)).isZero();
	}

	@Test
	void keepsArchivedLaneOutOfOrder() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String feitoId = createLane(token, boardId, "Feito");
		String descartadasId = createLane(token, boardId, "Descartadas");
		archive(descartadasId);

		putOrder(token, boardId, feitoId, aFazerId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name").value(contains("Feito", "A fazer")));

		assertThat(positionOf(descartadasId)).isNull();
		assertThat(archivedAtOf(descartadasId)).isNotNull();
	}

	@Test
	void acceptsEmptyOrderOnBoardWithoutActiveLanes() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		archive(createLane(token, boardId, "Descartadas"));

		putOrder(token, boardId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void keepsUpdatedAtOnReorder() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String feitoId = createLane(token, boardId, "Feito");
		OffsetDateTime aFazerUpdatedAt = updatedAtOf(aFazerId);
		OffsetDateTime feitoUpdatedAt = updatedAtOf(feitoId);

		putOrder(token, boardId, feitoId, aFazerId).andExpect(status().isOk());

		assertThat(updatedAtOf(aFazerId)).isEqualTo(aFazerUpdatedAt);
		assertThat(updatedAtOf(feitoId)).isEqualTo(feitoUpdatedAt);
	}

	static Stream<Named<OrderBody>> malformedOrderBodies() {
		return Stream.of(
				named("lane_ids ausente", ids -> "{}"),
				named("elemento null", ids -> """
						{"lane_ids": ["%s", null]}
						""".formatted(ids.feito())),
				named("id repetido", ids -> laneIdsBody(ids.feito(), ids.aFazer(), ids.feito())));
	}

	@ParameterizedTest
	@MethodSource("malformedOrderBodies")
	void rejectsMalformedOrder(OrderBody body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		LaneIds ids = boardWithLanes(token);

		mockMvc.perform(put(orderOf(ids.boardId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body.build(ids)))
				.andExpect(status().isBadRequest());

		assertThat(positionOf(ids.aFazer())).isZero();
		assertThat(positionOf(ids.feito())).isEqualTo(1);
	}

	static Stream<Named<OrderBody>> ordersWithDifferentLaneSet() {
		return Stream.of(
				named("lane ativa faltando", ids -> laneIdsBody(ids.feito())),
				named("lane de outro quadro", ids -> laneIdsBody(ids.feito(), ids.aFazer(), ids.backlog())),
				named("lane arquivada", ids -> laneIdsBody(ids.feito(), ids.aFazer(), ids.descartadas())),
				named("id inexistente",
						ids -> laneIdsBody(ids.feito(), ids.aFazer(), UUID.randomUUID().toString())));
	}

	@ParameterizedTest
	@MethodSource("ordersWithDifferentLaneSet")
	void rejectsOrderWithDifferentLaneSet(OrderBody body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		LaneIds ids = boardWithLanes(token);

		mockMvc.perform(put(orderOf(ids.boardId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body.build(ids)))
				.andExpect(status().isConflict());

		assertThat(positionOf(ids.aFazer())).isZero();
		assertThat(positionOf(ids.feito())).isEqualTo(1);
	}

	@Test
	void deletesLanesWithBoard() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "A fazer");
		archive(createLane(token, boardId, "Descartadas"));
		String otherBoardId = createBoard(token);
		String backlogId = createLane(token, otherBoardId, "Backlog");

		jdbcTemplate.update("delete from boards where id = ?", UUID.fromString(boardId));

		assertThat(countLanesOf(boardId)).isZero();
		assertThat(nameOf(backlogId)).isEqualTo("Backlog");
	}

	@Test
	void deletesLanesWithAccount() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);
		createLane(token, boardId, "A fazer");
		archive(createLane(token, boardId, "Descartadas"));

		jdbcTemplate.update("delete from accounts where email = ?", "ana@exemplo.com");

		assertThat(countLanesOf(boardId)).isZero();
	}

	@Test
	void documentsLaneEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].post.responses.201").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].post.responses.400").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].get.responses.400").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].get.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes'].get.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}'].put.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}'].put.responses.400").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}'].put.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}'].put.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}/archive'].post.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}/archive'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}/archive'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}/restore'].post.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}/restore'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/{laneId}/restore'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/order'].put.responses.200").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/order'].put.responses.400").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/order'].put.responses.401").exists())
				.andExpect(jsonPath("$.paths['/boards/{boardId}/lanes/order'].put.responses.409").exists())
				.andExpect(jsonPath("$.components.schemas.LaneResponse.properties.archived_at").exists())
				.andExpect(jsonPath("$.components.schemas.ReorderLanesRequest.properties.lane_ids").exists())
				.andExpect(jsonPath("$.components.schemas.ReorderLanesRequest.properties.laneIdsDistinct")
						.doesNotExist());
	}

	static Stream<Named<LaneItemRequest>> requestsOnLaneOfAnotherBoard() {
		return Stream.of(
				named("PUT /boards/{boardId}/lanes/{laneId}", (boardId, laneId) -> put(laneOf(boardId, laneId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Canceladas"}
								""")),
				named("POST /boards/{boardId}/lanes/{laneId}/archive",
						(boardId, laneId) -> post(laneOf(boardId, laneId) + "/archive")),
				named("POST /boards/{boardId}/lanes/{laneId}/restore",
						(boardId, laneId) -> post(laneOf(boardId, laneId) + "/restore")));
	}

	@ParameterizedTest
	@MethodSource("requestsOnLaneOfAnotherBoard")
	void hidesLaneOfAnotherBoard(LaneItemRequest request) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String firstBoardId = createBoard(token);
		String secondBoardId = createBoard(token);
		String backlogId = createLane(token, secondBoardId, "Backlog");

		mockMvc.perform(request.build(firstBoardId, backlogId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound());

		assertThat(nameOf(backlogId)).isEqualTo("Backlog");
		assertThat(archivedAtOf(backlogId)).isNull();
	}

	@Test
	void returnsNotFoundForUnknownLane() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String boardId = createBoard(token);

		putLane(token, boardId, UUID.randomUUID().toString(), "Backlog").andExpect(status().isNotFound());
	}

	static Stream<Named<LaneRequest>> requestsOnForeignBoard() {
		return Stream.of(
				named("POST /boards/{boardId}/lanes", (boardId, aFazerId, feitoId) -> post(lanesOf(boardId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Fazendo"}
								""")),
				named("GET /boards/{boardId}/lanes", (boardId, aFazerId, feitoId) -> get(lanesOf(boardId))),
				named("PUT /boards/{boardId}/lanes/{laneId}", (boardId, aFazerId, feitoId) -> put(laneOf(boardId, aFazerId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Backlog"}
								""")),
				named("POST /boards/{boardId}/lanes/{laneId}/archive",
						(boardId, aFazerId, feitoId) -> post(laneOf(boardId, aFazerId) + "/archive")),
				named("POST /boards/{boardId}/lanes/{laneId}/restore",
						(boardId, aFazerId, feitoId) -> post(laneOf(boardId, aFazerId) + "/restore")),
				named("PUT /boards/{boardId}/lanes/order", (boardId, aFazerId, feitoId) -> put(orderOf(boardId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(laneIdsBody(feitoId, aFazerId))));
	}

	@ParameterizedTest
	@MethodSource("requestsOnForeignBoard")
	void hidesLanesOfForeignBoard(LaneRequest request) throws Exception {
		String brunoToken = tokenOf("bruno@exemplo.com");
		String boardId = createBoard(brunoToken);
		String aFazerId = createLane(brunoToken, boardId, "A fazer");
		String feitoId = createLane(brunoToken, boardId, "Feito");
		List<Map<String, Object>> lanesBefore = lanesOf(UUID.fromString(boardId));
		String anaToken = tokenOf("ana@exemplo.com");
		String unknownBoardId = UUID.randomUUID().toString();

		String foreign = mockMvc.perform(request.build(boardId, aFazerId, feitoId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = mockMvc.perform(request.build(unknownBoardId, aFazerId, feitoId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(boardId, unknownBoardId)).isEqualTo(unknown);
		assertThat(lanesOf(UUID.fromString(boardId))).isEqualTo(lanesBefore);
	}

	@FunctionalInterface
	interface LaneRequest {

		MockHttpServletRequestBuilder build(String boardId, String aFazerId, String feitoId);

	}

	record LaneIds(String boardId, String aFazer, String feito, String descartadas, String backlog) {
	}

	@FunctionalInterface
	interface OrderBody {

		String build(LaneIds ids);

	}

	@FunctionalInterface
	interface LaneItemRequest {

		MockHttpServletRequestBuilder build(String boardId, String laneId);

	}

	private static String lanesOf(String boardId) {
		return "/boards/" + boardId + "/lanes";
	}

	private static String laneOf(String boardId, String laneId) {
		return lanesOf(boardId) + "/" + laneId;
	}

	private ResultActions putLane(String token, String boardId, String laneId, String name) throws Exception {
		return mockMvc.perform(put(laneOf(boardId, laneId))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "%s"}
						""".formatted(name)));
	}

	private LaneIds boardWithLanes(String token) throws Exception {
		String boardId = createBoard(token);
		String aFazerId = createLane(token, boardId, "A fazer");
		String feitoId = createLane(token, boardId, "Feito");
		String descartadasId = createLane(token, boardId, "Descartadas");
		archive(descartadasId);
		String backlogId = createLane(token, createBoard(token), "Backlog");
		return new LaneIds(boardId, aFazerId, feitoId, descartadasId, backlogId);
	}

	private static String orderOf(String boardId) {
		return lanesOf(boardId) + "/order";
	}

	private static String laneIdsBody(String... laneIds) {
		return """
				{"lane_ids": [%s]}
				""".formatted(Stream.of(laneIds).map("\"%s\""::formatted).collect(joining(", ")));
	}

	private ResultActions putOrder(String token, String boardId, String... laneIds) throws Exception {
		return mockMvc.perform(put(orderOf(boardId))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(laneIdsBody(laneIds)));
	}

	private ResultActions postAction(String token, String boardId, String laneId, String action) throws Exception {
		return mockMvc.perform(post(laneOf(boardId, laneId) + "/" + action)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions getLanes(String token, String boardId) throws Exception {
		return mockMvc.perform(get(lanesOf(boardId)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions getLanes(String token, String boardId, String archived) throws Exception {
		return mockMvc.perform(get(lanesOf(boardId)).param("archived", archived)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions postLane(String token, String boardId, String name) throws Exception {
		return mockMvc.perform(post(lanesOf(boardId))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "%s"}
						""".formatted(name)));
	}

	private String createLane(String token, String boardId, String name) throws Exception {
		String response = postLane(token, boardId, name)
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private String createBoard(String token) throws Exception {
		String project = mockMvc.perform(post("/projects")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Produto"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String response = mockMvc.perform(post("/projects/" + JsonPath.read(project, "$.id") + "/boards")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Sprint 12"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private String tokenOf(String email) throws Exception {
		if (countAccounts(email) == 0) {
			mockMvc.perform(post("/accounts")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"email": "%s", "display_name": "Ana", "password": "segredo"}
									""".formatted(email)))
					.andExpect(status().isCreated());
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

	/** O CHECK de lanes confere cada comando, então archived_at e position mudam juntos. */
	private void archive(String laneId) {
		jdbcTemplate.update("update lanes set archived_at = now(), position = null where id = ?",
				UUID.fromString(laneId));
	}

	private List<Map<String, Object>> lanesOf(UUID boardId) {
		return jdbcTemplate.queryForList(
				"select id, name, position, created_at, updated_at, archived_at from lanes where board_id = ? order by id",
				boardId);
	}

	private String nameOf(String laneId) {
		return jdbcTemplate.queryForObject(
				"select name from lanes where id = ?", String.class, UUID.fromString(laneId));
	}

	private Integer positionOf(String laneId) {
		return jdbcTemplate.queryForObject(
				"select position from lanes where id = ?", Integer.class, UUID.fromString(laneId));
	}

	private OffsetDateTime updatedAtOf(String laneId) {
		return jdbcTemplate.queryForObject(
				"select updated_at from lanes where id = ?", OffsetDateTime.class, UUID.fromString(laneId));
	}

	private OffsetDateTime archivedAtOf(String laneId) {
		return jdbcTemplate.queryForObject(
				"select archived_at from lanes where id = ?", OffsetDateTime.class, UUID.fromString(laneId));
	}

	private List<Integer> activePositionsOf(String boardId) {
		return jdbcTemplate.queryForList(
				"select position from lanes where board_id = ? and archived_at is null order by position",
				Integer.class, UUID.fromString(boardId));
	}

	private Integer countLanes() {
		return jdbcTemplate.queryForObject("select count(*) from lanes", Integer.class);
	}

	private Integer countLanesOf(String boardId) {
		return jdbcTemplate.queryForObject(
				"select count(*) from lanes where board_id = ?", Integer.class, UUID.fromString(boardId));
	}

	private Integer countAccounts(String email) {
		return jdbcTemplate.queryForObject(
				"select count(*) from accounts where email = ?", Integer.class, email);
	}

}
