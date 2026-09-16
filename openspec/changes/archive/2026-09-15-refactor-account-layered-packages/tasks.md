## 1. Linha de base

- [x] 1.1 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`, copiar `target/pit-reports/mutations.xml` para `.claude/tmp/pit-baseline-mutations.xml` e anotar nesta task o número de mutações com `status='KILLED'`. A task não produz código. Anotado: 335 de 337, com `clean` num worktree do commit base. Sem `clean`, o `target/classes` tinha classes compiladas pelo VS Code e deu 401 de 405, número que não se compara ao do javac.
- [x] 1.2 Rodar `./mvnw test` e anotar nesta task o total de `Tests run` e o de `AccountApiTest` e `AccountServiceTest`. A task não produz código. Anotado: 351 no total, 16 no `AccountApiTest` e 2 no `AccountServiceTest`.

## 2. Pacotes por camada

- [x] 2.1 Mover por `git mv` os arquivos de `account/` em `src/main` para os pacotes listados em Impact da proposal, `AccountApiTest` para `controller` e `AccountServiceTest` para `service` em `src/test`; ajustar `package` e imports, inclusive em `AuthService`, `ProjectMemberService`, `ProjectMemberView` e `GlobalExceptionHandler`; aplicar a visibilidade da proposal a todos os tipos, exceto `AccountMapper`
- [x] 2.2 Rodar `./mvnw test` e confirmar que passa com os números anotados em 1.2 e que não existe diretório `account/` em `src/main/java/com/william/kanban` nem em `src/test/java/com/william/kanban`

## 3. Email não normalizado recusado pelo banco

- [x] 3.1 Mover `databaseRejectsEmailNotNormalized` do `AccountApiTest` para o `AccountsTableTest`, com o mesmo corpo, e retirar do `AccountApiTest` os imports de `assertThatThrownBy` e `DataIntegrityViolationException`

```diff
+++ b/src/test/java/com/william/kanban/schema/AccountsTableTest.java
+@SpringBootTest
+@Import(TestcontainersConfiguration.class)
+class AccountsTableTest {
+
+	@Autowired
+	JdbcTemplate jdbcTemplate;
+
+	@Test
+	void databaseRejectsEmailNotNormalized() {
+	...
+	}
+
+}
```

- [x] 3.2 Rodar `./mvnw test -Dtest='AccountApiTest,AccountsTableTest' -Djacoco.skip=true` e confirmar que a soma de `Tests run` das duas classes é igual ao número de `AccountApiTest` anotado em 1.2

## 4. Entidade atrás do service

- [x] 4.1 Acrescentar ao `pom.xml` a propriedade `archunit.version` e a dependência `archunit-junit5`, criar o `ArchitectureTest` com os cinco campos do design, rodar `./mvnw test -Dtest=ArchitectureTest -Djacoco.skip=true` e confirmar `Tests run: 5`, com `onlyAccountServiceReadsPasswordHash` verde, `serviceOnlyAccessesItsLayers` e `mapperOnlyAccessesEntityAndDto` falhando com `Layer 'Mapper' is empty`, e `controllerOnlyAccessesServiceAndDto` e `entityAndRepositoryStayBehindService` falhando pela dependência de `AccountController` em `Account`

```diff
+++ b/pom.xml
 	<properties>
 ...
 		<pitest-junit5.version>1.2.3</pitest-junit5.version>
+		<archunit.version>1.5.0</archunit.version>
 	</properties>
 ...
 		<dependency>
 			<groupId>org.testcontainers</groupId>
 			<artifactId>testcontainers-junit-jupiter</artifactId>
 			<scope>test</scope>
 		</dependency>
 
+		<dependency>
+			<groupId>com.tngtech.archunit</groupId>
+			<artifactId>archunit-junit5</artifactId>
+			<version>${archunit.version}</version>
+			<scope>test</scope>
+		</dependency>
+
```

- [x] 4.2 Criar o `AccountMapper` e mudar `AccountService`, `AccountController` e `AccountServiceTest` conforme o design, com os quatro arquivos seguindo as regras de formatação de Convenções do `CLAUDE.md`: `var` em toda variável local inicializada na declaração, nenhuma linha acima de 90 caracteres, cadeia quebrada com uma chamada por linha um tab adiante e argumento em várias linhas fechando com `)` sozinho na indentação da linha que o abre, até `./mvnw test -Dtest=ArchitectureTest -Djacoco.skip=true` passar com `Tests run: 5`
- [x] 4.3 Rodar `./mvnw test` e confirmar que passa com o total anotado em 1.2 mais os cinco testes do `ArchitectureTest`, e que `find src/main/java/com/william/kanban/{controller,service,mapper,repository,entity,dto,exception} src/test/java/com/william/kanban/{controller,service,schema,ArchitectureTest.java} -name '*.java' | xargs awk '{ l = $0; gsub(/\t/, "    ", l); if (length(l) > 90) print FILENAME ":" FNR }'` não lista nenhuma linha

## 5. Documentação

- [x] 5.1 Reescrever no `CLAUDE.md` o parágrafo da capability `account-management` em Estado do projeto com os pacotes por camada, acrescentar o ArchUnit em Stack e, em Convenções, estender a todo o código as regras de formatação hoje restritas a teste (`var`, 90 caracteres por linha, quebra de cadeia, argumento, elemento de array e bloco de texto em várias linhas) e acrescentar os pacotes por camada com `dto` por feature, a conversão pelo mapper chamado só pelo service, as regras do `ArchitectureTest`, os pacotes de teste espelhando os de `src/main` e o teste de restrição do banco no pacote `schema`, uma classe por tabela. A task não produz código.
- [x] 5.2 Acrescentar o ArchUnit em Stack do `README.md` e trocar em Estrutura as linhas de `account/` pelos pacotes por camada, em `src/main` e em `src/test`, com `ArchitectureTest` e `schema/AccountsTableTest`. A task não produz código.

## 6. Mutação

- [x] 6.1 Rodar `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` e confirmar que o número de mutações com `status='KILLED'` em `target/pit-reports/mutations.xml` é maior ou igual ao anotado em 1.1. A task não produz código. Resultado: 337 de 339, com os mesmos dois sobreviventes da linha de base, em `OpenApiResponsesConfig` e `ProblemDetailAuthenticationEntryPoint`.
