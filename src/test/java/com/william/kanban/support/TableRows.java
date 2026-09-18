package com.william.kanban.support;

import org.springframework.jdbc.core.JdbcTemplate;

public final class TableRows {

	private final JdbcTemplate jdbcTemplate;

	public TableRows(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int count(String table) {
		return jdbcTemplate.queryForObject(
			"select count(*) from " + table, Integer.class
		);
	}

}
