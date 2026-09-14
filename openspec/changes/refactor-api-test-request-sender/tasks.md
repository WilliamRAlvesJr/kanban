## 1. Linha de base

- [ ] 1.1 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`, copiar `target/pit-reports/mutations.xml` para `.claude/tmp/pit-baseline-mutations.xml` e anotar nesta task o número de mutações com `status='KILLED'`. A task não produz código.
- [ ] 1.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true` e anotar nesta task o número de `Tests run`. A task não produz código.

## 2. POST com corpo e status

- [ ] 2.1 Criar `RequestSender` com `post`, `Request.body`, `Request.expectStatus` e `Response` sem método de verificação; no `AccountApiTest`, criar o `RequestSender` no `@BeforeEach`, criar as constantes `ACCOUNTS` e `ANA` e migrar `normalizesEmailToLowercase`, `storesPasswordAsBcryptHash`, `rejectsPayloadWithRequiredFieldMissingOrBlank`, `rejectsInvalidEmail`, `rejectsDuplicateEmail`, `rejectsDuplicateEmailInAnotherCase` e `createAna`

```diff
+++ b/src/test/java/com/william/kanban/account/AccountApiTest.java
 class AccountApiTest {
 
+	private static final String ACCOUNTS = "/accounts";
+
+	private static final String ANA = """
+			{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}
+			""";
+
 	@Autowired
 	MockMvc mockMvc;
 
 	@Autowired
 	JdbcTemplate jdbcTemplate;
 
+	RequestSender requestSender;
+
 	@BeforeEach
-	void cleanUp() {
+	void setUp() {
+		requestSender = new RequestSender(mockMvc);
 		jdbcTemplate.execute("delete from accounts");
 	}
 ...
 	@Test
+	void rejectsInvalidEmail() {
+		requestSender.post(ACCOUNTS)
+				.body("""
+						{"email": "ana.exemplo.com", "display_name": "Ana", "password": "segredo"}
+						""")
+				.expectStatus(BAD_REQUEST);
 
 		assertThat(countAccounts()).isZero();
 	}
```

- [ ] 2.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true` e confirmar que passa com o número de testes anotado em 1.2

## 3. Location, links e campos do corpo

- [ ] 3.1 Acrescentar ao `Response` `expectLocation`, `expectLinks` e as duas formas de `expectJson`; criar as constantes `ME` e `LOGIN` e migrar `createsAccount`

```diff
+++ b/src/test/java/com/william/kanban/account/AccountApiTest.java
 	@Test
+	void createsAccount() {
+		requestSender.post(ACCOUNTS)
+				.body(ANA)
+				.expectStatus(CREATED)
+				.expectLocation(ME)
+				.expectJson("$", aMapWithSize(1))
+				.expectLinks(
+						"self", ME,
+						"login", LOGIN);
 	}
```

- [ ] 3.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true` e confirmar que passa com o número de testes anotado em 1.2

## 4. GET com token e leitura de campo

- [ ] 4.1 Acrescentar ao `RequestSender` `get`, ao `Request` `as` e ao `Response` `expectPresent`, `expectAbsent` e `json`; migrar `doesNotReturnPassword`, `returnsAccountOfTokenWithLinks`, `documentsAccountEndpoints` e `tokenOfAna`, e retirar do `AccountApiTest` os imports que ficam sem uso
- [ ] 4.2 Rodar `./mvnw test -Dtest=AccountApiTest -Djacoco.skip=true`, confirmar que passa com o número de testes anotado em 1.2 e que `AccountApiTest` não tem `throws Exception` nem `mockMvc.perform`

## 5. Documentação

- [ ] 5.1 Acrescentar ao `CLAUDE.md`, em Convenções, o `RequestSender` de `com.william.kanban.support` e a criação dele no `@BeforeEach` a partir do `MockMvc`. A task não produz código.

## 6. Mutação

- [ ] 6.1 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o número de mutações com `status='KILLED'` em `target/pit-reports/mutations.xml` é maior ou igual ao anotado em 1.1. A task não produz código.
