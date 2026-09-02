# Kanban

API REST em Spring Boot para gestão de quadros kanban. O domínio ainda não está implementado: a
aplicação sobe e publica a documentação OpenAPI via Swagger UI, sem nenhum endpoint próprio.

## Requisitos

- JDK 21 ou superior (o build compila com `--release 21`)
- Maven não precisa estar instalado: use o wrapper do repositório

## Como rodar

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

| Recurso | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Especificação OpenAPI | http://localhost:8080/v3/api-docs |

```console
$ curl http://localhost:8080/v3/api-docs
{"openapi":"3.1.0","info":{"title":"OpenAPI definition","version":"v0"},...,"paths":{},"components":{}}
```

`paths` vazio é o estado esperado enquanto não houver controller.

Para subir em outra porta:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Para parar, encerre o processo `java.exe`. Interromper apenas o Maven deixa a aplicação viva
segurando a porta 8080, e a próxima subida falha com `Port 8080 was already in use`.

## Testes

```powershell
.\mvnw.cmd test                                              # todos
.\mvnw.cmd test -Dtest=KanbanApplicationTests                # uma classe
.\mvnw.cmd test -Dtest=KanbanApplicationTests#contextLoads   # um teste
```

`KanbanApplicationTests` é `@SpringBootTest` e verifica apenas a carga do contexto.

## Build

```powershell
.\mvnw.cmd clean package     # gera target/kanban-0.0.1-SNAPSHOT.jar
.\mvnw.cmd verify
```

## Stack

- Java 21, Spring Boot 4.1.1
- `spring-boot-starter-webmvc` (Tomcat e Jackson vêm junto)
- `springdoc-openapi-starter-webmvc-ui` 3.1.0 para OpenAPI e Swagger UI
- Testes com `spring-boot-starter-test` e `spring-boot-starter-webmvc-test`

A linha 3.x do springdoc é a compatível com Spring Boot 4; a 2.x atende o Boot 3.

Sem JPA e sem banco: adicionar o starter no `pom.xml` antes de escrever entidade ou repositório.

## Estrutura

```
src/main/java/com/william/kanban/
  KanbanApplication.java       ponto de entrada
src/main/resources/
  application.properties       spring.application.name
src/test/java/com/william/kanban/
  KanbanApplicationTests.java  carga do contexto
```

## Fluxo de mudanças

O repositório é spec-driven com OpenSpec (`openspec/`). As specs ficam em `openspec/specs/` e as
mudanças em andamento em `openspec/changes/`, no ciclo `/opsx:propose`, `/opsx:apply`,
`/opsx:verify`, `/opsx:archive`.

Change em andamento: `add-account-management`, ainda sem implementação.

O formato dos artefatos vem de `openspec/config.yaml`. As instruções para o agente estão no
`CLAUDE.md`.
