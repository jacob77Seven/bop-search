# Bop-Search Android

Package: `com.jacob77.bopsearch`  
minSdk 26 · targetSdk 34 · compileSdk **35** · Kotlin + Jetpack Compose + Material 3  
Playback: Jetpack Media3 ExoPlayer **1.5.1** + `MediaSessionService` (notification / lock screen / BT)

**Branch:** `feature/mixes-now-playing` (P0 — Mixes, Now playing, curation v1). Do not merge to `main` until Jacob smoke-tests.

## Open in Android Studio

1. **File → Open** → select this `android/` folder (Gradle project root).
2. Trust the project and wait for Gradle sync.
3. Run on emulator or device (API 26+; API 33+ for notification prompt).

## Tabs (Musicolet-style scrollable bottom nav)

| Tab | What it is |
|-----|------------|
| **Library** | SAF folders + app `files/library/` scan; tap to play |
| **Queues** | Active **play** queue (from Library / Mix play) |
| **Playlists** | P0 stub empty state (CRUD = P1) |
| **Mixes** | Create/edit/save Mix rules; **Play** fills queue + starts Media3 |
| **Now playing** | Fullscreen player (seek, skip, rating); mini-player bar when elsewhere |
| **Generate** | Prompt queue → PC drain (former Queue tab) |
| **Settings** | Peer host (`powerspec`), notifications deep-link |

## Mix evaluation (schema-locked)

- Contracts: `docs/schema/mix.schema.json`, `docs/schema/track.schema.json`
- Golden fixtures: `docs/schema/fixtures/mix-*.json`
- Kotlin: `domain/MixEvaluator.kt` (filter → score → sort)
- Score: `like_bias*(rating/100) + new_bias*(1 - min(play_count,10)/10)`; ties by `id` asc
- Verify offline (no Android SDK needed):

```bash
python3 tools/run_mix_fixtures.py
# expect 5/5 passed
```

- Or from `android/`: `./gradlew :app:testDebugUnitTest --tests com.jacob77.bopsearch.domain.MixEvaluatorFixtureTest`

## Jacob smoke-test (this branch)

1. **Checkout / install** `feature/mixes-now-playing`; Allow **POST_NOTIFICATIONS** when prompted (API 33+).
2. **Library** → Add music folder (SAF) or drop files into app `files/library/` → Rescan → play a track.
3. Confirm **mini-player** appears; tap it → **Now playing** fullscreen (title, play/pause, seek if duration known, ★ rating, skip).
4. Press **Home** — audio continues; shade / lock screen **Now playing** media controls still work (Media3 session).
5. **Mixes** → `+` → name e.g. `Favorites`, raise **Like bias**, optional rating min → **Save** → **Play**. Queues tab should list ordered tracks; playback starts.
6. On Now playing, **Skip** a few times — rating should **drop** (~5). Let a track finish — rating should **rise**; value shown on mini + fullscreen.
7. **Generate** → enqueue a prompt → Sync now (PC online via Tailscale `powerspec:8765`) — drain still works.
8. **Settings** → Notification settings deep-link still opens system UI.

### Optional fixture check before device test

```bash
# from repo root
python3 tools/run_mix_fixtures.py
```

## Configure peer

**Settings** tab: host `powerspec` (MagicDNS) or LAN/Tailscale IP, port `8765`.  
Android emulator → PC on the same machine: host `10.0.2.2`.

## Local library

1. App-private `files/library/` (Device File Explorer → Rescan).
2. **Add music folder** (SAF tree). Extensions: `.mp3` / `.m4a` / `.aac` / `.wav` / `.ogg` / `.flac` / `.opus`.

## Playback notes

- Play queue + Mix play use Media3 playlist (`setMediaItems`).
- Skip = next + curation rating down; natural end = full listen + rating up (0–100, mid 50).
- Genre/tone tags on phone library are thin in P0 — Mix genre/energy filters apply when metadata exists; untagged tracks still play for rating/like/new-only mixes.

## Logging

Logcat filters: `BopSync`, `BopPc`, `BopLibrary`, `BopPlayer`.
