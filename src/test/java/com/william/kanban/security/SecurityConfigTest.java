package com.william.kanban.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.william.kanban.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
// o pitest troca o bytecode com o contexto já em cache; recriar o contexto a cada teste
// faz os métodos @Bean rodarem de novo com a mutação
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class SecurityConfigTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void rejectsProtectedRouteWithoutToken() throws Exception {
		mockMvc.perform(get("/projects"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void hashesPasswordsWithBcrypt() {
		assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
	}

}
