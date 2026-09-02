# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

Esqueleto Spring Boot sem domínio implementado. Existem apenas `KanbanApplication` (classe `@SpringBootApplication`) e o teste `contextLoads`. Toda a modelagem do kanban está por fazer. A change `add-account-management` está proposta em `openspec/changes/`, ainda sem implementação.

## Stack

- Java 21, Spring Boot 4.1.1 (parent POM), Maven via wrapper
- `spring-boot-starter-webmvc` para REST e `springdoc-openapi-starter-webmvc-ui` 3.1.0 para OpenAPI e Swagger UI
- Sem JPA e sem banco: adicionar o starter no `pom.xml` antes de escrever entidade ou repositório
- Testes: `spring-boot-starter-test` e `spring-boot-starter-webmvc-test` (JUnit 5)

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
- Artefatos do OpenSpec e documentos do projeto são escritos em português do Brasil, com acentuação correta
