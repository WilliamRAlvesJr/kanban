# Kanban

API REST em Spring Boot para gestão de quadros kanban. Cadastro de conta, login por token e
quadros estão implementados (`POST /accounts`, `POST /auth/login`, `POST /auth/logout`,
`GET /accounts/me` e os endpoints de `/boards`); coluna e card ainda não.

## Requisitos

- JDK 21 ou superior (o build compila com `--release 21`)
- Docker, para o Postgres de desenvolvimento e para os testes
- Maven não precisa estar instalado: use o wrapper do repositório

Os comandos abaixo são para Git Bash.

## Como rodar

Suba o banco e exporte a conexão. Sem as três variáveis a aplicação falha ao criar o
`dataSource`, sem dizer qual delas falta.

```bash
docker compose up -d
export KANBAN_DB_URL=jdbc:postgresql://localhost:5432/kanban
export KANBAN_DB_USER=kanban
export KANBAN_DB_PASSWORD=kanban
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080` e o Flyway aplica as migrações de
`src/main/resources/db/migration`.

| Recurso | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Especificação OpenAPI | http://localhost:8080/v3/api-docs |

```console
$ curl -X POST http://localhost:8080/accounts -H "Content-Type: application/json" -d '{"email": "ana@exemplo.com", "display_name": "Ana", "password": "segredo"}'
{"id":"3f1b...","email":"ana@exemplo.com","display_name":"Ana"}

$ curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email": "ana@exemplo.com", "password": "segredo"}'
{"token":"kZ8x...","token_type":"Bearer","expires_at":"2026-09-09T00:12:34.567-03:00"}

$ curl http://localhost:8080/accounts/me -H "Authorization: Bearer kZ8x..."
{"id":"3f1b...","email":"ana@exemplo.com","display_name":"Ana"}

$ curl -X POST http://localhost:8080/boards -H "Authorization: Bearer kZ8x..." -H "Content-Type: application/json" -d '{"name": "Sprint 12", "description": "Trabalho da sprint"}'
{"id":"9c4e...","name":"Sprint 12","description":"Trabalho da sprint","created_at":"2026-09-10T18:00:00.123-03:00","updated_at":"2026-09-10T18:00:00.123-03:00","archived_at":null}

$ curl "http://localhost:8080/boards?archived=false" -H "Authorization: Bearer kZ8x..."
[{"id":"9c4e...","name":"Sprint 12","description":"Trabalho da sprint","created_at":"2026-09-10T18:00:00.123-03:00","updated_at":"2026-09-10T18:00:00.123-03:00","archived_at":null}]

$ curl -X POST http://localhost:8080/auth/logout -H "Authorization: Bearer kZ8x..."
```

O email é normalizado para minúsculas e a senha é gravada como hash BCrypt; nenhuma resposta
devolve a senha. `POST /accounts` responde 400 na entrada inválida e 409 no email repetido.

`POST /accounts`, `POST /auth/login` e a documentação OpenAPI atendem sem token; todo o resto
exige o header `Authorization: Bearer <token>` e responde 401 em `ProblemDetail` sem ele. O
token é opaco, aparece em claro só na resposta do login e fica no banco como hash SHA-256. Cada
login emite um token independente, e `POST /auth/logout` revoga o da própria chamada e responde
204. A validade é de 24 horas, pela propriedade `kanban.auth.token-ttl` do
`application.properties`.

Todo quadro pertence à conta do token. `GET /boards` lista do mais recente para o mais antigo e
aceita `archived=true` ou `archived=false`; `PUT /boards/{id}` substitui `name` e `description`;
`POST /boards/{id}/archive` e `POST /boards/{id}/restore` arquivam e restauram, e repetir a
operação não altera o quadro. `name` é obrigatório, com até 100 caracteres, e `description` tem
até 500. Quadro de outra conta responde 404, com o mesmo corpo de quadro inexistente.

Para subir em outra porta:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Para parar, encerre o processo `java.exe`. Interromper apenas o Maven deixa a aplicação viva
segurando a porta 8080, e a próxima subida falha com `Port 8080 was already in use`.

## Testes

Exigem Docker: o Testcontainers sobe um `postgres:17-alpine` por execução.

```bash
./mvnw test                                              # todos
./mvnw test -Dtest=AccountApiTest                        # uma classe
./mvnw test -Dtest=AccountApiTest#createsAccount         # um teste
```

## Cobertura

O JaCoCo já roda em `./mvnw test`: nenhuma flag extra, nenhum outro comando. O agente é
instrumentado antes dos testes e a própria fase `test` gera o relatório e aplica o gate.

O relatório fica em `target/site/jacoco/`:

| Arquivo | Uso |
| --- | --- |
| `target/site/jacoco/index.html` | relatório navegável, com o código-fonte anotado linha a linha |
| `target/site/jacoco/jacoco.csv` | uma linha por classe, para ler no terminal |
| `target/site/jacoco/jacoco.xml` | entrada para ferramentas externas |
| `target/jacoco.exec` | dados brutos da execução |

```bash
start target/site/jacoco/index.html    # abre o relatório no navegador
```

O número não enxerga regra declarativa. As anotações de Bean Validation do
`CreateAccountRequest` são metadado lido pelo Hibernate Validator, sem nenhum branch no código
do projeto: um payload inválido executa as mesmas instruções de um payload válido, seja qual for
o campo violado. Cobertura de 100% aqui significa "todo o código roda", não "toda regra tem
teste".

O `target/jacoco.exec` acumula as execuções: rodar um teste isolado não derruba o relatório, soma
ao que já estava lá. Para um número que corresponda só à rodada atual, use `./mvnw clean test`.

O gate reprova o build abaixo de **80% de instrução ou de branch** no projeto inteiro, com a
mensagem `Rule violated for bundle kanban`. `KanbanApplication` fica fora da medição. Para rodar
os testes sem medir nada:

```bash
./mvnw test -Djacoco.skip=true
```

## Mutação

O JaCoCo diz que a linha rodou; o PIT diz se algum teste repara quando ela muda. Ele altera o
bytecode (remove condicional, troca o retorno por `null` ou por objeto vazio) e reprova o teste
que segue verde. O conjunto de mutadores é o `STRONGER`, mais agressivo que o padrão.

```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

Não roda junto de `test`: a análise reexecuta a suíte por mutante e leva cerca de vinte minutos. O relatório fica em `target/pit-reports/index.html`, com cada
mutante listado sobre a linha que o originou. O build é reprovado abaixo de **80%** de mutantes
mortos; `KanbanApplication` fica de fora, como no JaCoCo.

O PIT também não enxerga regra declarativa: anotação e constraint de banco não viram bytecode
mutável, então o limite descrito acima vale igual aqui.

Código que o Spring executa uma vez por contexto, como os métodos `@Bean`, só tem os mutantes
mortos por teste que chama a classe direto (`OpenApiResponsesConfigTest`) ou que recria o contexto
(`SecurityConfigTest`). Com o contexto em cache, o PIT troca o bytecode depois que o bean já
existe, e o teste de ponta a ponta continua verde.

## Build

```bash
./mvnw clean package     # gera target/kanban-0.0.1-SNAPSHOT.jar
./mvnw verify
```

## Stack

- Java 21, Spring Boot 4.1.1
- `spring-boot-starter-webmvc` (Tomcat e Jackson vêm junto)
- `springdoc-openapi-starter-webmvc-ui` 3.1.0 para OpenAPI e Swagger UI
- `spring-boot-starter-data-jpa` sobre Postgres, com schema versionado por Flyway e
  `spring.jpa.hibernate.ddl-auto=validate`
- `spring-boot-starter-validation` e `spring-boot-starter-security`, este pelo
  `BCryptPasswordEncoder` e pela cadeia de filtros que resolve o token
- Testes com `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` e Testcontainers
- Cobertura com `jacoco-maven-plugin` 0.8.13

A linha 3.x do springdoc é a compatível com Spring Boot 4; a 2.x atende o Boot 3.

Em Spring Boot 4 a autoconfiguração de cada integração vem em módulo próprio: `flyway-core`
sozinho não migra nada sem `org.springframework.boot:spring-boot-flyway`.

## Estrutura

```
src/main/java/com/william/kanban/
  KanbanApplication.java          ponto de entrada
  account/                        entidade, repositório, serviço, controller e records de JSON
  auth/                           token, login, logout, filtro Bearer e cadeia de filtros
  board/                          entidade, repositório, serviço, controller e records de JSON
  shared/GlobalExceptionHandler   traduz as exceções em ProblemDetail
  shared/OpenApiResponsesConfig   documenta as respostas de erro a partir da assinatura
src/main/resources/
  application.properties          conexão por variável de ambiente e validação do schema
  db/migration/                   migrações do Flyway
src/test/java/com/william/kanban/
  TestcontainersConfiguration     Postgres em container para os testes
  KanbanApplicationTests          carga do contexto
  account/AccountApiTest          endpoints de conta, de ponta a ponta
  account/AccountServiceTest      consulta por id fora da API
  auth/AuthApiTest                login, logout e requisição autenticada
  auth/SecurityConfigTest         cadeia de filtros e encoder, com o contexto recriado a cada teste
  board/BoardApiTest              endpoints de quadro, de ponta a ponta
  shared/GlobalExceptionHandlerTest   tradução das exceções, sem contexto Spring
  shared/OpenApiResponsesConfigTest   regras de documentação do OpenAPI, sem contexto Spring
```

## Fluxo de mudanças

O repositório é spec-driven com OpenSpec (`openspec/`). As specs ficam em `openspec/specs/` e as
mudanças em andamento em `openspec/changes/`, no ciclo `/opsx:propose`, `/opsx:apply`,
`/opsx:verify`, `/opsx:archive`.

Specs publicadas: `account-management` e `authentication`. Não há change em andamento.

O formato dos artefatos vem de `openspec/config.yaml`. As instruções para o agente estão no
`CLAUDE.md`.
