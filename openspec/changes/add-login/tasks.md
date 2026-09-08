## 1. Dependência e schema

- [x] 1.1 Trocar a dependência de segurança no `pom.xml` e verificar que `./mvnw -q compile` passa com o `BCryptPasswordEncoder` ainda resolvido

```diff
+++ b/pom.xml
     <dependency>
-      <groupId>org.springframework.security</groupId>
-      <artifactId>spring-security-crypto</artifactId>
+      <groupId>org.springframework.boot</groupId>
+      <artifactId>spring-boot-starter-security</artifactId>
     </dependency>
```

- [x] 1.2 Criar `V2__create_auth_tokens.sql` e verificar que a aplicação sobe com `ddl-auto=validate` sem divergência de schema

```diff
+++ b/src/main/resources/db/migration/V2__create_auth_tokens.sql
CREATE TABLE auth_tokens (
    id         uuid        PRIMARY KEY,
    account_id uuid        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    token_hash text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);

CREATE UNIQUE INDEX ux_auth_tokens_token_hash ON auth_tokens (token_hash);
CREATE INDEX ix_auth_tokens_account_id ON auth_tokens (account_id);
```

- [ ] 1.3 Declarar `kanban.auth.token-ttl` no `application.properties` e verificar que o valor chega ao service como `Duration`

```diff
+++ b/src/main/resources/application.properties
kanban.auth.token-ttl=24h
```

## 2. Persistência do token

- [x] 2.1 Criar a entidade `AuthToken` em `com.william.kanban.auth`, com id gerado no construtor, e verificar que o contexto sobe com o schema validado

```diff
+++ b/src/main/java/com/william/kanban/auth/AuthToken.java
@Entity
@Table(name = "auth_tokens")
class AuthToken {

	@Id
	private UUID id;

	private UUID accountId;

	private String tokenHash;

	@Column(insertable = false, updatable = false)
	@Generated(event = EventType.INSERT)
	private OffsetDateTime createdAt;

	private OffsetDateTime expiresAt;
	...
}
```

- [ ] 2.2 Criar `AuthTokenRepository` com `findByTokenHash` e `deleteByTokenHash`, e verificar pelos testes de login e de logout

## 3. Verificação de credencial

- [ ] 3.1 Publicar o `PasswordEncoder` como bean e injetá-lo em `AccountService`, verificando que os testes de cadastro seguem verdes
- [ ] 3.2 Acrescentar `findByEmail` a `AccountRepository` e verificar que o login encontra a conta com o email em qualquer caixa
- [ ] 3.3 Tornar `AccountService` pública e acrescentar `authenticate`, que normaliza o email, devolve o id da conta e roda `matches` contra um hash fixo quando o email não existe; verificar pelos cenários "Senha errada" e "Email não cadastrado"

```diff
+++ b/src/main/java/com/william/kanban/account/AccountService.java
-class AccountService {
+public class AccountService {
...
+	public Optional<UUID> authenticate(String email, String password) {
+		...
+	}
```

## 4. Login e logout

- [ ] 4.1 Criar os records `LoginRequest` e `LoginResponse`, com `token_type` e `expires_at` em snake_case, e verificar o corpo devolvido no cenário "Credenciais corretas"
- [ ] 4.2 Criar `AuthService` com `login`, `logout` e `resolve`, gerando 256 bits por `SecureRandom` e gravando o SHA-256; verificar que a coluna `token_hash` não guarda o valor em claro
- [ ] 4.3 Criar `InvalidCredentialsException` e traduzi-la em `ProblemDetail` 401 no `GlobalExceptionHandler`, verificando que os dois cenários de credencial inválida devolvem corpos iguais
- [ ] 4.4 Criar `AuthController` com `POST /auth/login` e `POST /auth/logout`, anotando os códigos de resposta, e verificar pelos cenários de emissão e de revogação

## 5. Cadeia de filtros

- [ ] 5.1 Criar `BearerAuthenticationFilter` como `OncePerRequestFilter`, que lê o header `Authorization`, resolve o token e põe o id da conta no `SecurityContext`; verificar pelos cenários "Token válido", "Token desconhecido", "Header em outro formato" e "Token expirado"
- [ ] 5.2 Criar o `AuthenticationEntryPoint` que serializa `ProblemDetail` 401 com o `ObjectMapper` da aplicação, e verificar que a requisição sem header devolve 401 com corpo, não 403 vazio
- [ ] 5.3 Criar `SecurityConfig` com a `SecurityFilterChain` e verificar que cadastro, login e OpenAPI respondem sem token e que toda outra rota responde 401

```diff
+++ b/src/main/java/com/william/kanban/auth/SecurityConfig.java
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, BearerAuthenticationFilter filter) throws Exception {
		return http.csrf(CsrfConfigurer::disable)
				.httpBasic(HttpBasicConfigurer::disable)
				.formLogin(FormLoginConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(HttpMethod.POST, "/accounts", "/auth/login").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated())
				...
	}
```

## 6. Conta autenticada

- [ ] 6.1 Trocar `GET /accounts/{id}` por `GET /accounts/me` em `AccountController`, lendo o id por `@AuthenticationPrincipal`, e verificar pelos cenários "Conta do token" e "Conta de outro token"

```diff
+++ b/src/main/java/com/william/kanban/account/AccountController.java
-	@GetMapping("/{id}")
+	@GetMapping("/me")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Conta encontrada",
					content = @Content(schema = @Schema(implementation = AccountResponse.class))),
-			@ApiResponse(responseCode = "400", description = "Id fora do formato uuid",
-					content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
-			@ApiResponse(responseCode = "404", description = "Conta não encontrada",
+			@ApiResponse(responseCode = "401", description = "Token ausente, desconhecido ou expirado",
					content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
-	AccountResponse findById(@PathVariable UUID id) {
+	AccountResponse me(@AuthenticationPrincipal UUID accountId) {
		...
	}
```

- [ ] 6.2 Devolver 201 sem header `Location` no cadastro e verificar que o cenário "Conta criada" checa a ausência do header
- [ ] 6.3 Declarar o `securityScheme` `bearer` para o springdoc e verificar que o documento OpenAPI o traz e que o Swagger UI envia o header

## 7. Testes

- [ ] 7.1 Criar `AuthApiTest` cobrindo emissão, email em maiúsculas, campo ausente, senha errada, email não cadastrado, dois logins independentes, logout, logout sem token e token desconhecido
- [ ] 7.2 Cobrir "Token expirado" ajustando o `expires_at` da linha no banco antes da chamada
- [ ] 7.3 Atualizar `AccountApiTest`: remover os testes de consulta por id, mover `doesNotReturnPassword` para `/accounts/me`, checar a ausência de `Location` e atualizar `documentsAccountEndpoints`
- [ ] 7.4 Cobrir `AccountNotFoundException` por teste direto de `AccountService.findById` com um id desconhecido

## 8. Documentação e verificação

- [ ] 8.1 Atualizar `README.md` com o fluxo de login, os endpoints novos e a troca de dependência
- [ ] 8.2 Atualizar `CLAUDE.md` com o estado do projeto, a stack de segurança e a propriedade de prazo do token
- [ ] 8.3 Rodar `./mvnw -q verify` e confirmar que o gate de cobertura de 80% passa
- [ ] 8.4 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar 80% de mutantes mortos
