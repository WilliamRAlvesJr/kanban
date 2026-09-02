# Kanban

API REST em Spring Boot. No momento expõe um único endpoint, `GET /hello`, com a documentação
OpenAPI publicada via Swagger UI.

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
| Endpoint | http://localhost:8080/hello |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Especificação OpenAPI | http://localhost:8080/v3/api-docs |

```console
$ curl http://localhost:8080/hello
{"message":"Hello World"}
```

Para parar, encerre o processo `java.exe`. Interromper apenas o Maven deixa a aplicação viva
segurando a porta 8080, e a próxima subida falha com `Port 8080 was already in use`.

## Testes

```powershell
.\mvnw.cmd test                                          # todos
.\mvnw.cmd test -Dtest=HelloControllerTest               # uma classe
.\mvnw.cmd test -Dtest=OpenApiDocsTest#serveSwaggerUi    # um teste
```

`HelloControllerTest` usa a fatia `@WebMvcTest`; `OpenApiDocsTest` sobe a aplicação inteira em
porta aleatória e verifica que a especificação OpenAPI e a Swagger UI respondem.

## Build

```powershell
.\mvnw.cmd clean package     # gera target/kanban-0.0.1-SNAPSHOT.jar
.\mvnw.cmd verify
```

## Stack

- Java 21, Spring Boot 4.1.1
- `spring-boot-starter-webmvc` (Tomcat e Jackson vêm junto)
- `springdoc-openapi-starter-webmvc-ui` 3.1.0 para OpenAPI e Swagger UI

A linha 3.x do springdoc é a compatível com Spring Boot 4; a 2.x atende o Boot 3.

## Estrutura

```
src/main/java/com/william/kanban/
  KanbanApplication.java     ponto de entrada
  HelloController.java       GET /hello, devolve o record HelloResponse em JSON
src/test/java/com/william/kanban/
  KanbanApplicationTests.java  carga do contexto
  HelloControllerTest.java     fatia web do controller
  OpenApiDocsTest.java         OpenAPI e Swagger UI no ar
```

## Fluxo de mudanças

O repositório usa OpenSpec (`openspec/`). As specs ficam em `openspec/specs/` e as mudanças em
andamento em `openspec/changes/`, no ciclo propose, apply, archive.
