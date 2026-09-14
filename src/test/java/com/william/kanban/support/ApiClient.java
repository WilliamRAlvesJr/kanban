package com.william.kanban.support;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import org.hamcrest.Matcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

public final class ApiClient {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final MockMvc mockMvc;

	public ApiClient(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	public static Link link(String relation, String href) {
		return new Link(relation, href);
	}

	public Request get(String path) {
		return new Request(mockMvc, MockMvcRequestBuilders.get(path));
	}

	public Request post(String path) {
		return new Request(mockMvc, MockMvcRequestBuilders.post(path));
	}

	public record Link(String relation, String href) {
	}

	public static final class Request {

		private final MockMvc mockMvc;

		private final MockHttpServletRequestBuilder builder;

		private Request(MockMvc mockMvc, MockHttpServletRequestBuilder builder) {
			this.mockMvc = mockMvc;
			this.builder = builder;
		}

		public Request withToken(String token) {
			builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
			return this;
		}

		public Request withBody(Object body) {
			builder
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.writeValueAsString(body));
			return this;
		}

		public Response perform() {
			try {
				return new Response(mockMvc.perform(builder));
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

	}

	public static final class Response {

		private final ResultActions actions;

		private Response(ResultActions actions) {
			this.actions = actions;
		}

		public Response expectStatus(HttpStatus status) {
			return expect(status().is(status.value()));
		}

		public Response expectLocation(String path) {
			return expect(header().string(HttpHeaders.LOCATION, path));
		}

		public Response expectLinks(Link... links) {
			expect(jsonPath("$._links", aMapWithSize(links.length)));
			for (var link : links) {
				var hrefPath = "$._links['" + link.relation() + "'].href";
				expect(jsonPath(hrefPath).value(link.href()));
			}
			return this;
		}

		public Response expectJson(String path, Object value) {
			return expect(jsonPath(path).value(value));
		}

		public <T> Response expectJson(String path, Matcher<T> matcher) {
			return expect(jsonPath(path, matcher));
		}

		public Response expectPresent(String path) {
			return expect(jsonPath(path).exists());
		}

		public Response expectAbsent(String path) {
			return expect(jsonPath(path).doesNotExist());
		}

		public <T> T json(String path) {
			try {
				var body = actions.andReturn().getResponse().getContentAsString();
				return JsonPath.read(body, path);
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}

		private Response expect(ResultMatcher matcher) {
			try {
				actions.andExpect(matcher);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return this;
		}

	}

}
