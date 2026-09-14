## 1. Linha de base

- [x] 1.1 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`, copiar `target/pit-reports/mutations.xml` para `.claude/tmp/pit-baseline-mutations.xml` e anotar nesta task o número de mutações com `status='KILLED'`. A task não produz código. Anotado: 335 de 337.
- [x] 1.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true` e anotar nesta task o número de `Tests run`. A task não produz código. Anotado: 16.

## 2. POST com corpo e status

- [x] 2.1 Criar `ApiClient` com `post`, `Request.withBody`, `Request.perform` e `Response.expectStatus`; no `AccountApiTest`, criar o `ApiClient` no `@BeforeEach`, criar as constantes `ACCOUNTS` e `ANA` e migrar `normalizesEmailToLowercase`, `storesPasswordAsBcryptHash`, `rejectsPayloadWithRequiredFieldMissingOrBlank`, `rejectsInvalidEmail`, `rejectsDuplicateEmail`, `rejectsDuplicateEmailInAnotherCase` e `createAna`

```diff
+++ b/src/test/java/com/william/kanban/account/AccountApiTest.java
 class AccountApiTest {
 
+	private static final String ACCOUNTS = "/accounts";
+
+	private static final String ANA = """
+		{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}
+		""";
+
 	@Autowired
 	MockMvc mockMvc;
 
 	@Autowired
 	JdbcTemplate jdbcTemplate;
 
+	ApiClient api;
+
 	@BeforeEach
-	void cleanUp() {
+	void setUp() {
+		api = new ApiClient(mockMvc);
 		jdbcTemplate.execute("delete from accounts");
 	}
 ...
 	@Test
+	void rejectsInvalidEmail() {
+		api.post(ACCOUNTS)
+			.withBody("""
+				{"email": "ana.exemplo.com", "display_name": "Ana", "password": "segredo"}
+				""")
+			.perform()
+			.expectStatus(BAD_REQUEST);
 
 		assertThat(countAccounts()).isZero();
 	}
```

- [x] 2.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true` e confirmar que passa com o número de testes anotado em 1.2

## 3. Location, links e campos do corpo

- [x] 3.1 Acrescentar ao `ApiClient` o record `Link` e `link`, e ao `Response` `expectLocation`, `expectLinks` e as duas formas de `expectJson`; criar as constantes `ME` e `LOGIN` e migrar `createsAccount`

```diff
+++ b/src/test/java/com/william/kanban/account/AccountApiTest.java
 	@Test
+	void createsAccount() {
+		api.post(ACCOUNTS)
+			.withBody(ANA)
+			.perform()
+			.expectStatus(CREATED)
+			.expectLocation(ME)
+			.expectJson("$", aMapWithSize(1))
+			.expectLinks(
+				link("self", ME),
+				link("login", LOGIN)
+			);
 	}
```

- [x] 3.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true` e confirmar que passa com o número de testes anotado em 1.2

## 4. GET com token e leitura de campo

- [x] 4.1 Acrescentar ao `ApiClient` `get`, ao `Request` `withToken` e ao `Response` `expectPresent`, `expectAbsent` e `json`; migrar `doesNotReturnPassword`, `returnsAccountOfTokenWithLinks`, `documentsAccountEndpoints` e `tokenOfAna`, renomeado para `loginAsAna`, e retirar do `AccountApiTest` os imports que ficam sem uso
- [x] 4.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true`, confirmar que passa com o número de testes anotado em 1.2 e que `AccountApiTest` não tem `throws Exception` nem `mockMvc.perform`

## 5. Corpo da requisição pelos records de src/main

- [x] 5.1 Acrescentar o Lombok opcional ao `pom.xml`, com o processador em `annotationProcessorPaths` do `maven-compiler-plugin` e o `excludes` no `spring-boot-maven-plugin`; criar o `lombok.config`; acrescentar `@With` e `@JsonInclude(NON_NULL)` ao `CreateAccountRequest`, tornar `LoginRequest` público, trocar `withBody(String)` por `withBody(Object)` e montar todo corpo do `AccountApiTest` por esses records, com `ANA` virando constante `CreateAccountRequest` e o `@ValueSource` de `rejectsPayloadWithRequiredFieldMissingOrBlank` trocado por `@MethodSource("incompleteAccounts")`

```diff
+++ b/pom.xml
+		<dependency>
+			<groupId>org.projectlombok</groupId>
+			<artifactId>lombok</artifactId>
+			<optional>true</optional>
+		</dependency>
 ...
 			<plugin>
 				<groupId>org.springframework.boot</groupId>
 				<artifactId>spring-boot-maven-plugin</artifactId>
+				<configuration>
+					<excludes>
+						<exclude>
+							<groupId>org.projectlombok</groupId>
+							<artifactId>lombok</artifactId>
+						</exclude>
+					</excludes>
+				</configuration>
 			</plugin>
 
+			<plugin>
+				<groupId>org.apache.maven.plugins</groupId>
+				<artifactId>maven-compiler-plugin</artifactId>
+				<configuration>
+					<annotationProcessorPaths>
+						<path>
+							<groupId>org.projectlombok</groupId>
+							<artifactId>lombok</artifactId>
+							<version>${lombok.version}</version>
+						</path>
+					</annotationProcessorPaths>
+				</configuration>
+			</plugin>
```

```diff
+++ b/lombok.config
+config.stopBubbling = true
+lombok.addLombokGeneratedAnnotation = true
```

```diff
+++ b/src/main/java/com/william/kanban/account/CreateAccountRequest.java
+@With
+@JsonInclude(NON_NULL)
 record CreateAccountRequest(
```

```diff
+++ b/src/test/java/com/william/kanban/account/AccountApiTest.java
+	private static final CreateAccountRequest ANA =
+		new CreateAccountRequest("ana@exemplo.com", "Ana", "segredo");
 ...
+	static Stream<CreateAccountRequest> incompleteAccounts() {
+		return Stream.of(
+			ANA.withEmail(null),
+			ANA.withDisplayName(null),
+			ANA.withPassword(null),
+			ANA.withEmail(""),
+			ANA.withDisplayName(""),
+			ANA.withPassword("")
+		);
+	}
```

- [x] 5.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true` e confirmar que passa com o número de testes anotado em 1.2 e que `AccountApiTest` não tem JSON em string

## 6. Documentação

- [x] 6.1 Acrescentar ao `CLAUDE.md`, em Stack, o Lombok opcional e, em Convenções, o `ApiClient` de `com.william.kanban.support`, a criação dele no `@BeforeEach` a partir do `MockMvc`, o corpo de requisição por record e a formatação de cadeia e argumento em várias linhas no teste. A task não produz código.

## 7. Mutação

- [x] 7.1 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o número de mutações com `status='KILLED'` em `target/pit-reports/mutations.xml` é maior ou igual ao anotado em 1.1. A task não produz código. Resultado: 401 de 405.
