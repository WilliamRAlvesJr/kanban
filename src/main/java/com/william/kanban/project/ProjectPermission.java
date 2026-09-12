package com.william.kanban.project;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Constantes na ordem alfabética do valor JSON: a resposta lista as permissões na ordem de declaração. */
public enum ProjectPermission {

	@JsonProperty("add_boards")
	ADD_BOARDS,

	@JsonProperty("add_member")
	ADD_MEMBER,

	@JsonProperty("archive_project")
	ARCHIVE_PROJECT,

	@JsonProperty("edit_member")
	EDIT_MEMBER,

	@JsonProperty("edit_project")
	EDIT_PROJECT,

	@JsonProperty("remove_member")
	REMOVE_MEMBER,

	@JsonProperty("restore_project")
	RESTORE_PROJECT,

	@JsonProperty("view_boards")
	VIEW_BOARDS,

	@JsonProperty("view_member")
	VIEW_MEMBER,

	@JsonProperty("view_project")
	VIEW_PROJECT

}
