package com.william.kanban.shared;

import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

public final class LinksModel extends RepresentationModel<LinksModel> {

	public LinksModel(Link... links) {
		super(List.of(links));
	}

}
