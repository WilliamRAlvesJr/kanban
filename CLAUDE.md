# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

Esqueleto Spring Boot recém-gerado (Spring Initializr), sem nenhum commit ainda e sem domínio implementado. Existem apenas `KanbanApplication` (classe `@SpringBootApplication`) e o teste `contextLoads`. Toda a modelagem do kanban ainda está por fazer.

## Stack

- Java 21, Spring Boot 4.1.1 (parent POM), Maven via wrapper
- Única dependência de runtime: `spring-boot-starter` (sem web, sem JPA, sem banco). Adicionar starter no `pom.xml` antes de escrever controller, entidade ou repositório.
- Testes: `spring-boot-starter-test` (JUnit 5)

## Comandos

Use o wrapper (`./mvnw` no bash, `.\mvnw.cmd` no PowerShell); não há Maven global garantido.

```bash
./mvnw spring-boot:run          # sobe a aplicação
./mvnw test                     # todos os testes
./mvnw test -Dtest=KanbanApplicationTests            # uma classe
./mvnw test -Dtest=KanbanApplicationTests#contextLoads   # um teste
./mvnw clean package            # jar em target/
./mvnw -q verify                # build completo silencioso
```

Não há linter configurado.

## Fluxo OpenSpec

O repositório é spec-driven via OpenSpec (`openspec/config.yaml`, schema `spec-driven`). As specs vivem em `openspec/specs/`, as mudanças em andamento em `openspec/changes/`. O ciclo é: `/opsx:propose` (gera proposal, spec delta, design, tasks) → `/opsx:apply` (implementa as tasks) → `/opsx:archive`.

Regra do fluxo: a fase de proposta **não edita código de projeto**. Ao propor, pare depois dos artefatos de planejamento e espere um novo pedido do usuário para implementar, mesmo que o pedido original diga "construa" ou "corrija".

`openspec/config.yaml` está com todos os campos opcionais comentados: preencher `context:` ali (stack, convenções, domínio) é o que alimenta a geração dos artefatos.

## Convenções

- Pacote raiz `com.william.kanban`
- `HELP.md` está no `.gitignore` (é boilerplate do Initializr, não documentação viva)
- `pom.xml` mantém `<license>`, `<developers>` e `<scm>` vazios de propósito, para anular a herança do parent POM
