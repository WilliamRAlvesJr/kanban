## Context

<!-- Fill in: current structure (single flat module? layered by technical
concern — controllers/services/repositories?), what specifically makes
testing or swapping infrastructure hard today. -->

## Goals / Non-Goals

**Goals:**
- `{{domainModuleName}}` is testable without a real database, network,
  or framework test harness — only plain objects/functions and test
  doubles for `{{portsModuleName}}` interfaces.
- Dependencies point inward only: adapters depend on ports and domain;
  domain depends on nothing outside itself and its own ports.

**Non-Goals:**
- Not a rewrite of business logic's actual behavior — this migration
  changes where code lives and what it's allowed to import, not what it
  computes.
- Not migrating every module in one pass. Migrate one capability/feature
  at a time behind the same external interface, not a big-bang rewrite.

## Decisions

### Migration order: one capability/feature at a time, old and new structure coexisting until each is done

Rejected a big-bang rewrite: this repository's own
`docs/adr/0001-shared-core-two-delivery-targets.md`-style layering
decisions were made and rolled out incrementally in the same spirit —
large structural changes done feature-by-feature stay reviewable and
keep the codebase shippable throughout, rather than requiring a long-
lived branch. <Fill in the actual order this project will migrate in.>

### Port granularity: <fill in — e.g. one port per external dependency, or grouped by capability>

<!-- Rejected alternatives and why. -->

## Risks / Trade-offs

- **[Risk]** Mid-migration, some code still directly imports
  infrastructure while other code goes through ports — a temporary
  inconsistency. → **Mitigation**: accepted as an expected, visible state
  during a feature-by-feature migration; track which capabilities have
  moved in this change's own `tasks.md`, not left implicit.
