# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

A capability `account-management` está implementada. O pacote `com.william.kanban.account` tem `Account`, `AccountRepository`, `AccountService`, `AccountController`, `AccountNotFoundException` e os records `CreateAccountRequest` e `AccountResponse`. `POST /accounts` responde 201 com o email em minúsculas e a senha em hash BCrypt, 400 na entrada inválida e 409 no email repetido; `GET /accounts/{id}` responde 200, 400 no id fora do formato uuid e 404 no id desconhecido. O `GlobalExceptionHandler`, em `com.william.kanban.shared`, converte essas exceções em `ProblemDetail`.

A capability `authentication` está em construção. A tabela `auth_tokens` e o pacote `com.william.kanban.auth`, com `AuthToken` e `AuthTokenRepository`, já existem; enquanto não houver `SecurityConfig`, a autoconfiguração do Spring Security bloqueia toda rota e `./mvnw test` reprova. Board, coluna e card estão por fazer.

## Stack

- Java 21, Spring Boot 4.1.1 (parent POM), Maven via wrapper
- `spring-boot-starter-webmvc` para REST e `springdoc-openapi-starter-webmvc-ui` 3.1.0 para OpenAPI e Swagger UI
- Persistência em Postgres com `spring-boot-starter-data-jpa`; schema versionado por Flyway em `src/main/resources/db/migration`, com `spring.jpa.hibernate.ddl-auto=validate`
- Em Spring Boot 4 a autoconfiguração de cada integração vem em módulo próprio: `flyway-core` sozinho não migra nada sem `org.springframework.boot:spring-boot-flyway`
- `spring-boot-starter-validation` para as anotações de Bean Validation e `spring-boot-starter-security` pelo `BCryptPasswordEncoder` e pela cadeia de filtros
- Testes: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` (JUnit 5) e Testcontainers, que sobem um `postgres:17-alpine` por `TestcontainersConfiguration`
- Teste de mutação pelo `pitest-maven` 1.30.0 com `pitest-junit5-plugin` 1.2.3, fora do ciclo padrão: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` gera `target/pit-reports/` e reprova abaixo de 80% de mutantes mortos
- Cobertura pelo `jacoco-maven-plugin`: a fase `test` gera o relatório em `target/site/jacoco/` e roda o `check`, que reprova o build abaixo de 80% de instrução ou de branch; `KanbanApplication` fica fora da medição

## Comandos

Use o wrapper `./mvnw`, no Git Bash: é o shell do projeto, e não há Maven global garantido.
No Git Bash a forma `.\mvnw.cmd` falha com `.mvnw.cmd: command not found`, porque a barra
invertida escapa o `m`.

`./mvnw test` exige Docker: os testes de contexto sobem um Postgres em container.

Antes de `spring-boot:run`, suba o banco de desenvolvimento e exporte a conexão. Sem as três variáveis a aplicação falha ao criar o `dataSource`, sem dizer qual falta.

```bash
docker compose up -d            # kanban-postgres na porta 5432
export KANBAN_DB_URL=jdbc:postgresql://localhost:5432/kanban
export KANBAN_DB_USER=kanban
export KANBAN_DB_PASSWORD=kanban
```

```bash
./mvnw spring-boot:run          # sobe a aplicação
./mvnw test                     # todos os testes
./mvnw test -Dtest=KanbanApplicationTests            # uma classe
./mvnw test -Dtest=KanbanApplicationTests#contextLoads   # um teste
./mvnw test -Djacoco.skip=true  # testes sem medir cobertura
./mvnw clean package            # jar em target/
./mvnw -q verify                # build completo silencioso
```

Não há linter configurado.

## Fluxo OpenSpec

O repositório é spec-driven via OpenSpec (`@fission-ai/openspec`, schema `spec-driven`). As specs vivem em `openspec/specs/`, as mudanças em andamento em `openspec/changes/`, os templates de change em `openspec/templates/`. O ciclo é: `/opsx:propose` (gera proposal, spec delta, design, tasks) → `/opsx:apply` (implementa as tasks) → `/opsx:verify` → `/opsx:archive`.

O profile instalado é `custom`, com 11 workflows: além do ciclo acima, `explore`, `new`, `continue`, `ff`, `update`, `sync` e `bulk-archive`. Os slash commands ficam em `.claude/commands/opsx/` e as skills em `.claude/skills/openspec-*`, gerados por `openspec update` a partir da lista de workflows na config global (`%APPDATA%/openspec/config.json`) e versionados no repositório.

Regra do fluxo: a fase de proposta **não edita código de projeto**. Ao propor, pare depois dos artefatos de planejamento e espere um novo pedido do usuário para implementar, mesmo que o pedido original diga "construa" ou "corrija".

`openspec/config.yaml` alimenta a geração dos artefatos e é o lugar de mexer no formato deles. Ele não repete stack, comandos nem convenções: aponta para este arquivo.

- `context:` traz as regras de escrita dos artefatos e as de diagrama. Vale para todo artefato gerado.
- `rules.proposal` e `rules.design` trazem o que é específico de cada um.
- As regras de escrita cortam racional de decisão, trabalho futuro, estado anterior e hedge. As de diagrama pedem Mermaid no lugar da prosa sempre que o conteúdo for fluxo, sequência, estado ou modelo de dados.

`openspec/agent-harness.json` fixa `autonomyLevel: assisted` e `reviewGate: human-required`.

## Hooks

`.claude/settings.json` liga um hook `PreToolUse` em `Bash`, filtrado por `if: "Bash(git commit *)"`, que roda `.claude/hooks/verificar-claude-md.py`.

O script lê `git diff --cached`. Libera o commit quando o índice está vazio ou já contém o `CLAUDE.md`; senão nega uma vez, listando os arquivos que vão para o commit, e grava a marca do índice em `.claude/tmp/claude-md-conferido-<sessão>.txt`. A segunda tentativa com o mesmo índice passa, então o gate não entra em laço.

O `CLAUDE.md` descreve o estado atual: stack, comandos, convenções, estrutura e fluxo. Não recebe motivo de mudança, comparação com o que era antes nem histórico.

## Convenções

- Pacote raiz `com.william.kanban`
- `pom.xml` mantém `<license>`, `<developers>` e `<scm>` vazios de propósito, para anular a herança do parent POM
- `.openspec-ui/` e `.claude/tmp/` estão no `.gitignore` (estado local da UI do OpenSpec e temporários do Claude Code)
- Código em inglês: pacote, classe, método, variável, coluna, tabela, endpoint e campo de JSON
- Campo de JSON em snake_case e campo Java em camelCase: o record leva `@JsonProperty("display_name")` onde o nome tem mais de uma palavra, e a anotação vale tanto para o Jackson 3 da aplicação quanto para o schema que o springdoc gera com Jackson 2
- Prosa em português do Brasil, com acentuação correta: artefatos do OpenSpec, documentos do projeto, comentário e Javadoc, mensagem de commit
