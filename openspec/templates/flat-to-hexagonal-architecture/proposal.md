## Why

<!-- Fill in: what forces this change now — business logic currently
depends directly on a framework/database/HTTP client that is hard to
test or swap, or a specific pain point with the current flat/layered
structure. -->

## What Changes

- Introduce a `{{domainModuleName}}` module containing business logic
  with zero imports from frameworks, database clients, or HTTP/transport
  libraries.
- Introduce a `{{portsModuleName}}` module defining the interfaces
  (abstract classes/protocols/traits — whatever this language's idiom
  is) that `{{domainModuleName}}` depends on for anything external
  (persistence, external APIs, clock/time, etc.).
- Introduce an `{{adaptersModuleName}}` module containing the concrete
  implementations of those ports (the actual database client, HTTP
  client, framework-specific code) — this is the only place framework/
  infrastructure imports are allowed.
- Move existing business logic out of framework-coupled code
  (controllers/handlers/views) into `{{domainModuleName}}`, with the
  framework layer reduced to translating requests into domain calls and
  domain results into responses.

## Capabilities

### Modified Capabilities

- <fill in: which existing capability's implementation moves behind this
  new layering, without changing its external behavior>

## Impact

- New: `{{domainModuleName}}/`, `{{portsModuleName}}/`,
  `{{adaptersModuleName}}/`.
- Modified: existing framework-layer code (controllers/handlers/views),
  reduced to request/response translation calling into
  `{{domainModuleName}}`.
- No external-behavior change — this is an internal restructuring; every
  existing route/entry point should behave identically before and after.
