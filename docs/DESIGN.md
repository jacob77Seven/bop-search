# Bop-Search design

Living product/engineering design for `jacob77Seven/bop-search`.
Android + PC share one product brain; UIs can differ in toolkit but not in rules.

Inspired by local players like [Musicolet](https://play.google.com/store/apps/details?id=in.krosbits.musicolet) (efficient bottom nav, strong queues/playlists, lock-screen controls, offline-first) plus Bop-Search’s generation + curation loop.

## Goals

1. **Sleek, feature-rich local player** on Android (Musicolet-class navigation density).
2. **PC app** with the same library, mixes, curation, and playback concepts — plus ComfyUI generation host.
3. **One domain / sync model** so features are not invented twice.
4. **Opportunistic WAN** via Tailscale (`powerspec`); phone works offline and drains when PC is up.
5. **Ship on feature branches**, merge to `main` after Jacob smoke-tests.

## Non-goals (near term)

- Streaming/search from the public internet (generation is on the home PC).
- Perfect Musicolet clone (equalizer, Android Auto, widgets — later).
- Always-on PC assumption.

---

## Information architecture (Android)

Bottom row (scrollable if needed), Musicolet-style one-tap destinations:

| Tab | Role |
|-----|------|
| **Library** | Folders / all tracks / search; add SAF music folders |
| **Queues** | Active play queue(s); resume positions |
| **Playlists** | User-ordered lists |
| **Mixes** | Rule-based auto queues (tags / tone / energy / rating / newness) |
| **Now playing** | Compact bar + **tap → fullscreen player**; survives background |
| **Generate** | Prompt queue → PC jobs; recent prompts; auto-gen settings |
| **Settings** | Peer host (`powerspec`), notifications, download policy, blacklist |

PC mirrors the same tabs where they make sense; Generate + library sync are first-class on PC.

### Now playing

- Compact mini-player always reachable (bottom or Now playing tab).
- Tap expands to **fullscreen**: artwork/placeholder, title, seek, play/pause, next/prev (when queue exists), rating + skip/curation affordances, open queue.
- Back / peek collapses without stopping audio.
- Playback via Media3 `MediaSessionService` (notification / lock screen / BT) — already on `main`.
- Process death: later add playback resumption; v1 keeps foreground service while playing.

---

## Domain model (shared)

Implement once (see reuse below); both clients speak the same shapes over sync.

### Track

- Stable id: content fingerprint (segment hashes) + path hints  
- Paths: phone URI(s), PC path(s)  
- Tags: genre (tree), tone, energy  
- Rating (float or 0–100; default mid)  
- Blacklist flag / trash state  
- Play counts, last played, date added  
- Optional: user notes

### Genre tree

- Nested nodes; user-defined; tracks point at one or more leaves/nodes.

### Playlist

- Ordered track ids; manual.

### Mix (rule set)

Saved configuration, not a frozen track list:

- Genre: allow / require / exclude (tree-aware)  
- Tone / energy ranges  
- Rating min (and optional max)  
- **Like bias** slider: weight toward high-rated  
- **New music bias** slider: weight toward unseen / low play-count / recently generated  
- Optional seed / shuffle salt  
- Name + icon optional  

Evaluating a Mix → ordered or weighted candidate set → feed the play queue.

### Curation events

- Full listen → rating up (configurable step)  
- Skip → rating down  
- Undo via “previous”: first back undoes skip penalty / bumps previous (Jacob’s hijack-back behavior)  
- Below threshold → blacklist; PC deletes or moves to trash per setting  

### Generation job

- Prompt, client_id, local_id (idempotent drain)  
- Status: queued / accepted / running / ready / failed  
- Output path(s) on PC → download to phone when Wi‑Fi (default) or per data setting  

---

## Feature roadmap (priority)

### P0 — now (feature branches)

1. **UI shell refresh** — bottom nav + Mixes tab stub + Now playing mini → fullscreen.  
2. **Mixes** — create/edit/save mix rules; play mix → queue; persist locally.  
3. **Curation v1** — skip/full-listen rating updates; show rating on now-playing; sync rating deltas to PC when online.  
4. **Shared schema doc** — JSON schemas for Track / Mix / CurationEvent / Job (Donald + Chuck).

### P1 — next

5. Playlist CRUD + add-to-playlist from now-playing.  
6. Queue UX (clear, reorder, “play next”).  
7. PC player shell reusing same Mix/curation evaluation (Python or shared lib).  
8. Download pipeline: PC finished job → phone library + fingerprint id.  
9. Auto-tagging on PC (genre/tone/energy) → sync tags to phone.

### P2 — later

10. Genre tree editor; NLP/TTS verbal curation (optional).  
11. Equalizer / gapless / multi-queue like Musicolet.  
12. Widgets, Android Auto.  
13. ComfyUI wiring behind existing `/v1/jobs`.

---

## Cross-platform reuse (Donald to refine)

**Share (must):** Mix evaluation, rating/curation rules, fingerprint identity, sync protocol, genre tree semantics, job queue semantics.

**Don’t share naively:** UI widgets, Media3 vs desktop audio backends.

**Candidate approaches (rank after Donald’s memo):**

| Approach | Pros | Cons |
|----------|------|------|
| A. **Kotlin Multiplatform** shared module + Android app + Compose Desktop or thin PC UI | One language with existing Android | PC today is Python FastAPI |
| B. **PC as source of truth API** + both UIs as clients | Fits current FastAPI; one evaluation engine on PC | Phone offline Mix play needs local copy of rules/engine |
| C. **Shared JSON schemas + dual impl** of small pure functions (Kotlin + Python) | Fast to start | Drift risk — mitigate with golden tests |
| D. **Rust/Go sync + rules daemon** both call | Strong single engine | Extra stack |

**Pragmatic default for P0:** C for Mix/curation pure functions + schemas in `docs/schema/`, with a plan to collapse to A or B once Mix rules stabilize. Phone must evaluate Mixes **offline**.

---

## Sync protocol (sketch)

- Phone-local SQLite (Room): tracks, mixes, playlists, queue, pending curation, pending jobs.  
- When PC online (health): push pending jobs + curation; pull track metadata / new files manifest; download blobs per Wi‑Fi policy.  
- Conflict: last-write-wins on rating with vector or timestamp; fingerprint id wins over path.

---

## Git workflow

- `main` — Jacob-tested, always buildable.  
- `feature/<name>` — e.g. `feature/mixes-ui`, `feature/now-playing`.  
- PR or merge only after Jacob smoke-test on device.  
- Steve coordinates; Chuck implements; Donald researches / schemas / ComfyUI later.

---

## Acceptance snapshots

**Mixes P0:** create a mix with genre allow + rating min + like/new sliders; start it; tracks enqueue; survive app background; rating updates on skip/listen.  

**Now playing P0:** mini player; tap fullscreen; media notification still works; no crash on Home.  

**Reuse P0:** `docs/schema/` published; Donald memo linked from this file.

---

## Open questions (Steve will decide defaults if blocked)

1. Desktop UI toolkit (Compose Desktop vs keep Python + webview vs Qt) — default pending Donald.  
2. Rating scale 1–5 vs 0–100 — **default 0–100**, mid 50.  
3. Multiple concurrent queues (Musicolet-style) — **P1**; P0 single queue + Mixes regenerating into it.

---

*Last updated: 2026-09-18 — Steve*
