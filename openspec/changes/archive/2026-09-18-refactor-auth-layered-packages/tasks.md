## 1. Linha de base

- [x] 1.1 Rodar `./mvnw clean test-compile org.pitest:pitest-maven:mutationCoverage`, copiar `target/pit-reports/mutations.xml` para `.claude/tmp/pit-baseline-mutations.xml` e anotar nesta task o número de mutações com `status='KILLED'`. A task não produz código.
  - Linha de base: 337 mutações `KILLED`.
- [x] 1.2 Rodar `./mvnw test` e anotar nesta task o total de `Tests run` e o de `AuthApiTest`, `SecurityConfigTest` e `ArchitectureTest`. A task não produz código.
  - Linha de base: 356 testes no total; `AuthApiTest` 22, `SecurityConfigTest` 2, `ArchitectureTest` 5.

## 2. Pacotes por camada

- [x] 2.1 Mover por `git mv` os arquivos de `auth/` em `src/main` para os pacotes listados em Impact da proposal, `AuthApiTest` para `controller` e `SecurityConfigTest` para `security` em `src/test`; ajustar `package` e imports, inclusive em `GlobalExceptionHandler` e `AccountApiTest`; aplicar a visibilidade da proposal a todos os tipos, mantendo `IssuedToken` em `dto/auth` até o grupo 4
- [x] 2.2 Rodar `./mvnw test` e confirmar que passa com os números anotados em 1.2 e que não existe diretório `auth/` em `src/main/java/com/william/kanban` nem em `src/test/java/com/william/kanban`

## 3. Security só acessa service

- [x] 3.1 Acrescentar ao `ArchitectureTest` a constante `SECURITY`, a camada `Security` em `layers()` e o campo `securityOnlyAccessesService`

```diff
+++ b/src/test/java/com/william/kanban/ArchitectureTest.java
+	private static final String SECURITY = "com.william.kanban.security..";
+
 ...
+	@ArchTest
+	static final ArchRule securityOnlyAccessesService = layers()
+		.whereLayer("Security")
+		.mayOnlyAccessLayers("Service");
+
 ...
 			.layer("Exception")
-			.definedBy(EXCEPTION);
+			.definedBy(EXCEPTION)
+			.layer("Security")
+			.definedBy(SECURITY);
```

- [x] 3.2 Rodar `./mvnw test -Dtest=ArchitectureTest -Djacoco.skip=true` e confirmar `Tests run: 6`, todos verdes

## 4. Login devolve LoginResponse

- [x] 4.1 Mudar `AuthService.login` e `AuthController.login` conforme o design e apagar `IssuedToken`
- [x] 4.2 Rodar `./mvnw test -Dtest='AuthApiTest,ArchitectureTest' -Djacoco.skip=true` e confirmar que passa com o número de `AuthApiTest` anotado em 1.2 e seis testes no `ArchitectureTest`

## 5. Formatação

- [x] 5.1 Aplicar aos arquivos movidos de `src/main` e ao `SecurityConfigTest` as regras de formatação de Convenções do `CLAUDE.md`: `var` em toda variável local inicializada na declaração, nenhuma linha fora de `import` acima de 90 caracteres, cadeia quebrada com uma chamada por linha um tab adiante, argumento e lista de parâmetros em várias linhas fechando com `)` sozinho na indentação da linha que os abre
- [x] 5.2 Rodar `./mvnw test` e confirmar que passa com o total anotado em 1.2 mais o campo novo do `ArchitectureTest`, e que `find src/main/java/com/william/kanban/security src/main/java/com/william/kanban/dto/auth src/main/java/com/william/kanban/{controller/AuthController,service/AuthService,repository/AuthTokenRepository,entity/AuthToken,exception/InvalidCredentialsException}.java src/test/java/com/william/kanban/security -name '*.java' | xargs awk '/^import / { next } { l = $0; sub(/\r$/, "", l); gsub(/\t/, "    ", l); if (length(l) > 90) print FILENAME ":" FNR }'` não lista nenhuma linha

## 6. Documentação

- [x] 6.1 Reescrever no `CLAUDE.md` o parágrafo da capability `authentication` em Estado do projeto com os pacotes por camada, o pacote `security` e os records `LoginRequest` e `LoginResponse` em `dto.auth`; em Convenções, tirar `auth` da lista de pacotes por feature, acrescentar `security` como pacote da configuração do Spring Security, acrescentar às regras do `ArchitectureTest` a de `security` acessando só `service`; e excluir a linha de `import` do limite de 90 caracteres. A task não produz código.
- [x] 6.2 Trocar em Estrutura do `README.md` a linha de `auth/` em `src/main` pelas classes de auth nos pacotes por camada e por `security/`, e as linhas de `auth/AuthApiTest` e `auth/SecurityConfigTest` em `src/test` por `controller/AuthApiTest` e `security/SecurityConfigTest`. A task não produz código.

## 7. Mutação

- [x] 7.1 Acrescentar ao `SecurityConfigTest.rejectsProtectedRouteWithoutToken` a conferência do `Content-Type` `application/problem+json`
- [x] 7.2 Rodar `./mvnw clean test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o número de mutações com `status='KILLED'` em `target/pit-reports/mutations.xml` é maior ou igual ao anotado em 1.1. A task não produz código.
