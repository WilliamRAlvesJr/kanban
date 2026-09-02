## 1. Scaffolding

- [ ] 1.1 Create the `{{domainModuleName}}`, `{{portsModuleName}}`, and
  `{{adaptersModuleName}}` modules (empty, with whatever this language's
  module/package boundary marker is).
- [ ] 1.2 Add a lint/build rule (or, if unavailable, a documented
  convention + code-review checklist item) preventing
  `{{domainModuleName}}` from importing framework/database/HTTP code
  directly.

## 2. First capability migration (proves the pattern)

- [ ] 2.1 Pick one existing capability/feature to migrate first. Define
  its ports in `{{portsModuleName}}`.
- [ ] 2.2 Move its business logic into `{{domainModuleName}}`, taking
  ports as dependencies instead of concrete infrastructure.
- [ ] 2.3 Implement the concrete adapters in `{{adaptersModuleName}}`.
- [ ] 2.4 Update the framework layer (controller/handler/view) to call
  into `{{domainModuleName}}` via the wired adapters, with no behavior
  change to the external interface.
- [ ] 2.5 Confirm existing tests for this capability still pass
  unmodified (external behavior unchanged) and add domain-level tests
  that no longer need a real database/network.

## 3. Remaining capabilities

- [ ] 3.1 Repeat step 2 for each remaining capability, one at a time.

## 4. Cleanup

- [ ] 4.1 Once every capability has migrated, remove any now-unused
  direct-infrastructure-access code paths left in the old structure.
