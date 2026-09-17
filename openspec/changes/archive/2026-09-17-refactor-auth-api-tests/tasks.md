## 1. Linha de base

- [x] 1.1 Rodar `./mvnw clean test-compile org.pitest:pitest-maven:mutationCoverage`, copiar `target/pit-reports/mutations.xml` para `.claude/tmp/pit-baseline-mutations.xml` e anotar nesta task o número de mutações com `status='KILLED'`. A task não produz código. Linha de base: 337 mortos.
- [x] 1.2 Rodar `./mvnw test` e anotar nesta task o total de `Tests run` e o de `AuthApiTest` e `RootApiTest`. A task não produz código. Linha de base: 356 no total, 22 no `AuthApiTest` e 6 no `RootApiTest`.

## 2. Login e logout sem header manual

- [x] 2.1 Acrescentar ao `ApiClient` `expectNoLocation` e `body`, com `json` lendo o corpo por `body`; no `AuthApiTest`, criar o `ApiClient` no `@BeforeEach`, criar as constantes do design e os helpers `createAccount`, `createAna` e `tokenOf`, e migrar `issuesTokenForCorrectCredentials`, `issuesTokenForEmailInAnotherCase`, `rejectsWrongPassword`, `answersUnknownEmailWithTheSameBodyAsWrongPassword`, `issuesIndependentTokenOnEachLogin`, `storesOnlyTheHashOfTheToken`, `acceptsValidToken`, `returnsTheAccountOfTheTokenUsed`, `acceptsTokenIssuedAnHourAgo`, `rejectsRequestWithoutAuthorizationHeader`, `rejectsUnknownToken`, `rejectsExpiredToken`, `logoutRevokesTheTokenUsed`, `logoutKeepsTheOtherTokens`, `rejectsLogoutWithoutToken` e `documentsAuthEndpoints`

```diff
+++ b/src/test/java/com/william/kanban/auth/AuthApiTest.java
 	@Test
-	void issuesTokenForCorrectCredentials() throws Exception {
+	void issuesTokenForCorrectCredentials() {
 		createAna();
 
+		api.post(LOGIN)
+			.withBody(ANA_LOGIN)
+			.perform()
+			.expectStatus(CREATED)
+			.expectJson("$.token", not(emptyOrNullString()))
+			.expectJson("$.token_type", "Bearer")
+			.expectJson("$.expires_at", not(emptyOrNullString()))
+			.expectLinks(
+				link("me", ME),
+				link("logout", LOGOUT)
+			)
+			.expectNoLocation();
 	}
```

- [x] 2.2 Rodar `./mvnw test -Dtest=AuthApiTest -Djacoco.skip=true` e confirmar que passa com o número anotado em 1.2

## 3. Header Authorization em outro esquema

- [x] 3.1 Acrescentar ao `ApiClient` `withHeader`, com `withToken` delegando a ele, e migrar `rejectsAuthorizationHeaderInAnotherFormat` e `rejectsValidTokenUnderAnotherScheme`

```diff
+++ b/src/test/java/com/william/kanban/support/ApiClient.java
+		public Request withHeader(String name, String value) {
+			builder.header(name, value);
+			return this;
+		}
+
 		public Request withToken(String token) {
-			builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
-			return this;
+			return withHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
 		}
```

- [x] 3.2 Rodar `./mvnw test -Dtest=AuthApiTest -Djacoco.skip=true` e confirmar que passa com o número anotado em 1.2

## 4. Corpo de login pelo LoginRequest

- [x] 4.1 Acrescentar `@With` e `@JsonInclude(NON_NULL)` ao `LoginRequest`, migrar `rejectsPayloadWithRequiredFieldMissingOrBlank` para `@MethodSource("incompleteLogins")` e retirar do `AuthApiTest` o `mockMvc.perform`, o `throws Exception`, o JSON em string e os imports sem uso

```diff
+++ b/src/main/java/com/william/kanban/auth/LoginRequest.java
+@With
+@JsonInclude(NON_NULL)
 public record LoginRequest(
```

- [x] 4.2 Rodar `./mvnw test -Dtest='AuthApiTest,AccountApiTest' -Djacoco.skip=true`, confirmar que o `AuthApiTest` passa com o número anotado em 1.2 e que `grep -nE 'throws Exception|mockMvc\.perform|\{"' src/test/java/com/william/kanban/auth/AuthApiTest.java` não lista nenhuma linha

## 5. Pontos de entrada

- [x] 5.1 Migrar todos os testes do `RootApiTest` conforme o design e retirar dele o `mockMvc.perform`, o `throws Exception`, o JSON em string e os imports sem uso
- [x] 5.2 Rodar `./mvnw test -Dtest=RootApiTest -Djacoco.skip=true`, confirmar que passa com o número anotado em 1.2 e que `grep -nE 'throws Exception|mockMvc\.perform|\{"' src/test/java/com/william/kanban/shared/RootApiTest.java` não lista nenhuma linha

## 6. Formatação

- [x] 6.1 Rodar `./mvnw test` e confirmar que passa com o total anotado em 1.2, e que `find src/test/java/com/william/kanban/{auth/AuthApiTest.java,shared/RootApiTest.java,support/ApiClient.java} src/main/java/com/william/kanban/auth/LoginRequest.java | xargs awk '{ l = $0; gsub(/\t/, "    ", l); if (length(l) > 90) print FILENAME ":" FNR }'` não lista nenhuma linha

## 7. Mutação

- [x] 7.1 Rodar `./mvnw clean test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o número de mutações com `status='KILLED'` em `target/pit-reports/mutations.xml` é maior ou igual ao anotado em 1.1. A task não produz código.
