# Bop-Search shared schemas

JSON Schema stubs for the shared product brain. Android (Kotlin) and PC (Python FastAPI) should validate against these and keep Mix/curation behavior aligned via golden fixtures.

| File | Purpose |
|------|---------|
| `track.schema.json` | Track identity, tags, rating |
| `mix.schema.json` | Rule-based Mix definition |
| `curation-event.schema.json` | Listen / skip / rate events |
| `job.schema.json` | Generation jobs |
| `sync-envelope.schema.json` | Push/pull sync batch |
| `fixtures/` | Mix evaluation golden tests |

Rating scale: **0–100** (mid 50). Phone evaluates Mixes **offline** against the local track index.
