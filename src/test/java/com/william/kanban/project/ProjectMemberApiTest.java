package com.william.kanban.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.william.kanban.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProjectMemberApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.execute("delete from accounts");
	}

	@Test
	void addsAccountAsMember() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		UUID bia = accountIdOf("bia@exemplo.com");

		addMember(token, projectId, "Bia@Exemplo.com")
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		getMembers(token, projectId)
				.andExpect(jsonPath("$._embedded.members", hasSize(1)))
				.andExpect(jsonPath("$._embedded.members[0].account_id").value(bia.toString()))
				.andExpect(jsonPath("$._embedded.members[0].email").value("bia@exemplo.com"))
				.andExpect(jsonPath("$._embedded.members[0].permissions", hasSize(0)));
	}

	@Test
	void listsMembers() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		insertMember(projectId, "bia@exemplo.com");
		insertMember(projectId, "caio@exemplo.com");

		getMembers(token, projectId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.members[*].email")
						.value(containsInAnyOrder("bia@exemplo.com", "caio@exemplo.com")));
	}

	@Test
	void listsNewestMemberFirst() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		for (String email : List.of("bia@exemplo.com", "caio@exemplo.com", "davi@exemplo.com")) {
			accountIdOf(email);
			addMember(token, projectId, email).andExpect(status().isNoContent());
		}

		getMembers(token, projectId)
				.andExpect(jsonPath("$._embedded.members[*].email")
						.value(contains("davi@exemplo.com", "caio@exemplo.com", "bia@exemplo.com")));
	}

	@Test
	void keepsOwnerOutOfMemberList() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		getMembers(token, createProject(token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.members", hasSize(0)));
	}

	@Test
	void listsOnlyMembersOfProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String produto = createProject(token);
		String operacoes = createProject(token);
		insertMember(operacoes, "bia@exemplo.com");

		getMembers(token, produto)
				.andExpect(jsonPath("$._embedded.members[*].email").value(not(hasItem("bia@exemplo.com"))));
	}

	@Test
	void linksMemberListForOwner() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);

		getMembers(token, projectId)
				.andExpect(jsonPath("$._links", aMapWithSize(3)))
				.andExpect(jsonPath("$._links.self.href").value("/projects/" + projectId + "/members"))
				.andExpect(jsonPath("$._links['add-member'].href").value("/projects/" + projectId + "/members"))
				.andExpect(jsonPath("$._links.project.href").value("/projects/" + projectId));
	}

	@Test
	void returnsNotFoundForUnknownProjectOnMemberList() throws Exception {
		getMembers(tokenOf("ana@exemplo.com"), UUID.randomUUID().toString())
				.andExpect(status().isNotFound());
	}

	@Test
	void returnsMember() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		UUID bia = accountIdOf("bia@exemplo.com", "Bia");
		String memberId = insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT", "ADD_MEMBER");

		getMember(token, projectId, memberId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(memberId))
				.andExpect(jsonPath("$.account_id").value(bia.toString()))
				.andExpect(jsonPath("$.email").value("bia@exemplo.com"))
				.andExpect(jsonPath("$.display_name").value("Bia"))
				.andExpect(jsonPath("$.created_at").isNotEmpty())
				.andExpect(jsonPath("$.permissions").value(contains("add_member", "view_project")));
	}

	@Test
	void hidesMemberOfAnotherProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String produto = createProject(token);
		String operacoes = createProject(token);
		String memberId = insertMember(operacoes, "bia@exemplo.com");
		String unknownId = UUID.randomUUID().toString();

		String foreign = getMember(token, produto, memberId)
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = getMember(token, produto, unknownId)
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(memberId, unknownId)).isEqualTo(unknown);
	}

	@Test
	void returnsNotFoundForUnknownMember() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		getMember(token, createProject(token), UUID.randomUUID().toString())
				.andExpect(status().isNotFound());
	}

	@Test
	void linksMemberForOwner() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com");
		String self = "/projects/" + projectId + "/members/" + memberId;

		getMember(token, projectId, memberId)
				.andExpect(jsonPath("$._links", aMapWithSize(3)))
				.andExpect(jsonPath("$._links.self.href").value(self))
				.andExpect(jsonPath("$._links.remove.href").value(self))
				.andExpect(jsonPath("$._links['edit-permissions'].href").value(self + "/permissions"));
	}

	@Test
	void embedsSameMemberAsSingleRead() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT");

		String list = getMembers(token, projectId)
				.andExpect(jsonPath("$._embedded.members", hasSize(1)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String single = getMember(token, projectId, memberId)
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(JsonPath.<Map<String, Object>>read(list, "$._embedded.members[0]"))
				.isEqualTo(JsonPath.<Map<String, Object>>read(single, "$"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"zeca@exemplo.com", "ana@exemplo.com", "bia@exemplo.com"})
	void ignoresEmailWithoutEffect(String email) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT");
		List<Map<String, Object>> before = membershipOf(projectId);

		addMember(token, projectId, email)
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertThat(membershipOf(projectId)).isEqualTo(before);
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"email\": \"\"}", "{\"email\": \"bia.exemplo.com\"}"})
	void rejectsInvalidAddPayload(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);

		mockMvc.perform(post("/projects/" + projectId + "/members")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(countMembers()).isZero();
	}

	@Test
	void rejectsRepeatedLinkInDatabase() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com");

		assertThatThrownBy(() -> insertMember(projectId, "bia@exemplo.com"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void replacesPermissions() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT", "ADD_MEMBER");

		putPermissions(token, projectId, memberId, """
				["view_member", "edit_project"]""")
				.andExpect(status().isOk());

		getMember(token, projectId, memberId)
				.andExpect(jsonPath("$.permissions").value(contains("edit_project", "view_member")));
	}

	@Test
	void clearsPermissions() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT");

		putPermissions(token, projectId, memberId, "[]").andExpect(status().isOk());

		getMember(token, projectId, memberId).andExpect(jsonPath("$.permissions", hasSize(0)));
	}

	@Test
	void storesRepeatedPermissionOnce() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com");

		putPermissions(token, projectId, memberId, """
				["view_project", "view_project"]""")
				.andExpect(status().isOk());

		getMember(token, projectId, memberId).andExpect(jsonPath("$.permissions").value(contains("view_project")));
	}

	@Test
	void doesNotReplacePermissionsOfMemberOfAnotherProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String produto = createProject(token);
		String operacoes = createProject(token);
		String memberId = insertMember(operacoes, "bia@exemplo.com", "VIEW_PROJECT");

		putPermissions(token, produto, memberId, "[]").andExpect(status().isNotFound());

		assertThat(permissionsOf(memberId)).containsExactly("VIEW_PROJECT");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"{\"permissions\": null}",
			"{\"permissions\": \"view_project\"}",
			"{\"permissions\": [\"delete_project\"]}",
			"{\"permissions\": [null]}"})
	void rejectsInvalidPermissionsPayload(String body) throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT");

		mockMvc.perform(put("/projects/" + projectId + "/members/" + memberId + "/permissions")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(permissionsOf(memberId)).containsExactly("VIEW_PROJECT");
	}

	@Test
	void respondsToPermissionsWriteWithSelfOnly() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com");

		putPermissions(token, projectId, memberId, "[]")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$._links", aMapWithSize(1)))
				.andExpect(jsonPath("$._links.self.href").value("/projects/" + projectId + "/members/" + memberId));
	}

	@Test
	void removesMember() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String projectId = createProject(token);
		String memberId = insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT");

		deleteMember(token, projectId, memberId)
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		getMember(token, projectId, memberId).andExpect(status().isNotFound());
		mockMvc.perform(get("/projects/" + projectId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("bia@exemplo.com")))
				.andExpect(status().isNotFound());
		assertThat(permissionsOf(memberId)).isEmpty();
	}

	@Test
	void doesNotRemoveMemberOfAnotherProject() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String produto = createProject(token);
		String operacoes = createProject(token);
		String memberId = insertMember(operacoes, "bia@exemplo.com");

		deleteMember(token, produto, memberId).andExpect(status().isNotFound());

		getMember(token, operacoes, memberId).andExpect(status().isOk());
	}

	@ParameterizedTest
	@CsvSource({
			"ADD_MEMBER, ADD, 204",
			"VIEW_MEMBER, LIST, 200",
			"VIEW_MEMBER, GET, 200",
			"REMOVE_MEMBER, DELETE, 204",
			"EDIT_MEMBER, PUT, 200"})
	void allowsMemberWithEndpointPermission(String permission, String request, int expected) throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com", permission);
		String caio = insertMember(projectId, "caio@exemplo.com");
		accountIdOf("davi@exemplo.com");

		mockMvc.perform(memberRequestOf(request, projectId, caio)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("bia@exemplo.com")))
				.andExpect(status().is(expected));
	}

	@ParameterizedTest
	@ValueSource(strings = {"ADD", "LIST", "GET", "DELETE", "PUT"})
	void deniesMemberWithoutEndpointPermission(String request) throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com");
		String caio = insertMember(projectId, "caio@exemplo.com", "VIEW_PROJECT");
		accountIdOf("davi@exemplo.com");
		List<Map<String, Object>> before = membershipOf(projectId);

		mockMvc.perform(memberRequestOf(request, projectId, caio)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf("bia@exemplo.com")))
				.andExpect(status().isForbidden());

		assertThat(membershipOf(projectId)).isEqualTo(before);
	}

	@Test
	void doesNotImplyAnotherPermission() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com", "ADD_MEMBER", "REMOVE_MEMBER", "EDIT_MEMBER");

		getMembers(tokenOf("bia@exemplo.com"), projectId).andExpect(status().isForbidden());
	}

	@Test
	void allowsOwnerWithoutPermission() throws Exception {
		String token = tokenOf("ana@exemplo.com");

		getMembers(token, createProject(token)).andExpect(status().isOk());
	}

	@Test
	void checksPermissionBeforeMemberOfUrl() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com", "VIEW_MEMBER");

		deleteMember(tokenOf("bia@exemplo.com"), projectId, UUID.randomUUID().toString())
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = {"LIST", "DELETE"})
	void hidesMembersFromAccountOutsideProject(String request) throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		String bia = insertMember(projectId, "bia@exemplo.com");
		List<Map<String, Object>> before = membershipOf(projectId);
		String brunoToken = tokenOf("bruno@exemplo.com");
		String unknownId = UUID.randomUUID().toString();

		String foreign = mockMvc.perform(memberRequestOf(request, projectId, bia)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + brunoToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = mockMvc.perform(memberRequestOf(request, unknownId, bia)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + brunoToken))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(projectId, unknownId)).isEqualTo(unknown);
		assertThat(membershipOf(projectId)).isEqualTo(before);
	}

	@Test
	void memberChangesOwnPermissions() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		String bia = insertMember(projectId, "bia@exemplo.com", "EDIT_MEMBER");
		String biaToken = tokenOf("bia@exemplo.com");

		putPermissions(biaToken, projectId, bia, """
				["edit_member", "view_project"]""")
				.andExpect(status().isOk());

		mockMvc.perform(get("/projects/" + projectId).header(HttpHeaders.AUTHORIZATION, "Bearer " + biaToken))
				.andExpect(status().isOk());
	}

	@Test
	void memberRemovesItself() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		String bia = insertMember(projectId, "bia@exemplo.com", "REMOVE_MEMBER");

		deleteMember(tokenOf("bia@exemplo.com"), projectId, bia).andExpect(status().isNoContent());

		assertThat(membershipOf(projectId)).isEmpty();
	}

	@Test
	void linksMemberListForMember() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com", "VIEW_MEMBER");

		getMembers(tokenOf("bia@exemplo.com"), projectId)
				.andExpect(jsonPath("$._links", aMapWithSize(1)))
				.andExpect(jsonPath("$._links.self.href").value("/projects/" + projectId + "/members"));
	}

	@ParameterizedTest
	@CsvSource({
			"VIEW_MEMBER, self",
			"VIEW_MEMBER REMOVE_MEMBER, self remove",
			"VIEW_MEMBER EDIT_MEMBER, self edit-permissions"})
	void linksMemberByPermissions(String permissions, String links) throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com", permissions.split(" "));
		String caio = insertMember(projectId, "caio@exemplo.com");

		String body = getMember(tokenOf("bia@exemplo.com"), projectId, caio)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(JsonPath.<Map<String, Object>>read(body, "$._links").keySet())
				.containsExactlyInAnyOrder(links.split(" "));
	}

	@Test
	void hidesMembersFromAccountOutsideProjectOnAdd() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		insertMember(projectId, "bia@exemplo.com");
		accountIdOf("caio@exemplo.com");
		List<Map<String, Object>> before = membershipOf(projectId);
		String brunoToken = tokenOf("bruno@exemplo.com");
		String unknownId = UUID.randomUUID().toString();

		String foreign = addMember(brunoToken, projectId, "caio@exemplo.com")
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String unknown = addMember(brunoToken, unknownId, "caio@exemplo.com")
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(foreign.replace(projectId, unknownId)).isEqualTo(unknown);
		assertThat(membershipOf(projectId)).isEqualTo(before);
	}

	@Test
	void deletesMembersWithAccount() throws Exception {
		String token = tokenOf("ana@exemplo.com");
		String first = insertMember(createProject(token), "bia@exemplo.com", "VIEW_PROJECT");
		String second = insertMember(createProject(token), "bia@exemplo.com", "VIEW_PROJECT");

		jdbcTemplate.update("delete from accounts where email = ?", "bia@exemplo.com");

		assertThat(countMembers()).isZero();
		assertThat(permissionsOf(first)).isEmpty();
		assertThat(permissionsOf(second)).isEmpty();
	}

	@Test
	void deletesMembersWithProject() throws Exception {
		String projectId = createProject(tokenOf("ana@exemplo.com"));
		String bia = insertMember(projectId, "bia@exemplo.com", "VIEW_PROJECT");
		String caio = insertMember(projectId, "caio@exemplo.com", "VIEW_PROJECT");

		jdbcTemplate.update("delete from projects where id = ?", UUID.fromString(projectId));

		assertThat(countMembers()).isZero();
		assertThat(permissionsOf(bia)).isEmpty();
		assertThat(permissionsOf(caio)).isEmpty();
	}

	@Test
	void documentsMemberEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].post.responses.204").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].post.responses.400").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].post.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].post.responses.403").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].post.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].get.responses.401").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].get.responses.403").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members'].get.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].get.responses.200").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].get.responses.403").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].get.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].delete.responses.204").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].delete.responses.403").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].delete.responses.404").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}/permissions'].put.responses.200")
						.exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}/permissions'].put.responses.400")
						.exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}/permissions'].put.responses.403")
						.exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}/permissions'].put.responses.404")
						.exists())
				.andExpect(jsonPath("$.components.schemas.EntityModelProjectMemberResponse.properties.display_name")
						.exists())
				.andExpect(jsonPath("$.components.schemas.UpdateProjectMemberPermissionsRequest.properties.permissions")
						.exists());
	}

	private static MockHttpServletRequestBuilder memberRequestOf(String request, String projectId, String memberId) {
		String members = "/projects/" + projectId + "/members";
		return switch (request) {
			case "ADD" -> post(members)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"email": "davi@exemplo.com"}
							""");
			case "LIST" -> get(members);
			case "GET" -> get(members + "/" + memberId);
			case "DELETE" -> delete(members + "/" + memberId);
			case "PUT" -> put(members + "/" + memberId + "/permissions")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"permissions": []}
							""");
			default -> throw new IllegalArgumentException(request);
		};
	}

	private ResultActions deleteMember(String token, String projectId, String memberId) throws Exception {
		return mockMvc.perform(delete("/projects/" + projectId + "/members/" + memberId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions putPermissions(String token, String projectId, String memberId, String permissions)
			throws Exception {
		return mockMvc.perform(put("/projects/" + projectId + "/members/" + memberId + "/permissions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"permissions": %s}
						""".formatted(permissions)));
	}

	private List<String> permissionsOf(String memberId) {
		return jdbcTemplate.queryForList(
				"select permission from project_member_permissions where project_member_id = ? order by permission",
				String.class, UUID.fromString(memberId));
	}

	private ResultActions getMember(String token, String projectId, String memberId) throws Exception {
		return mockMvc.perform(get("/projects/" + projectId + "/members/" + memberId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions getMembers(String token, String projectId) throws Exception {
		return mockMvc.perform(get("/projects/" + projectId + "/members")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private ResultActions addMember(String token, String projectId, String email) throws Exception {
		return mockMvc.perform(post("/projects/" + projectId + "/members")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s"}
						""".formatted(email)));
	}

	private String insertMember(String projectId, String email, String... permissions) throws Exception {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("insert into project_members (id, project_id, account_id, created_at) values (?, ?, ?, now())",
				id, UUID.fromString(projectId), accountIdOf(email));
		for (String permission : permissions) {
			jdbcTemplate.update("insert into project_member_permissions (project_member_id, permission) values (?, ?)",
					id, permission);
		}
		return id.toString();
	}

	private List<Map<String, Object>> membershipOf(String projectId) {
		return jdbcTemplate.queryForList("""
				select m.id, m.account_id, p.permission
				from project_members m
				left join project_member_permissions p on p.project_member_id = m.id
				where m.project_id = ?
				order by m.id, p.permission""", UUID.fromString(projectId));
	}

	private String createProject(String token) throws Exception {
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
		return location.substring(location.lastIndexOf('/') + 1);
	}

	private UUID accountIdOf(String email) throws Exception {
		return accountIdOf(email, "Conta");
	}

	private UUID accountIdOf(String email, String displayName) throws Exception {
		if (countAccounts(email) == 0) {
			mockMvc.perform(post("/accounts")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"email": "%s", "display_name": "%s", "password": "segredo"}
									""".formatted(email, displayName)))
					.andExpect(status().isCreated());
		}
		return jdbcTemplate.queryForObject("select id from accounts where email = ?", UUID.class, email);
	}

	private String tokenOf(String email) throws Exception {
		accountIdOf(email);
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

	private Integer countMembers() {
		return jdbcTemplate.queryForObject("select count(*) from project_members", Integer.class);
	}

	private Integer countAccounts(String email) {
		return jdbcTemplate.queryForObject("select count(*) from accounts where email = ?", Integer.class, email);
	}

}
