# Bop-Search

Offline-first music player that uses **your own hardware** for generation and
listening feedback for curation.

- **Phone** works fully offline: local queue + local library playback.
- When a **PC** comes online (typically over Tailscale Personal / MagicDNS),
  the phone probes it and drains pending queue jobs over plain HTTP.
- Generation (ComfyUI, etc.) is intentionally out of scope for this first slice.

```
┌─────────────────┐         HTTP (Tailscale / LAN)        ┌─────────────────┐
│  Android phone  │  ── probe /health, POST /v1/jobs ──►  │  PC stub (this) │
│  offline queue  │                                       │  FastAPI :8765  │
│  local library  │                                       │  (ComfyUI later)│
└─────────────────┘                                       └─────────────────┘
```

## Layout

| Path | Role |
|------|------|
| `android/` | Kotlin + Jetpack Compose app (`com.jacob77.bopsearch`) |
| `pc/` | Python FastAPI presence + job-accept stub |
| `LICENSE` | MIT |
| `.gitignore` | Android + Python defaults |

## Architecture (this slice)

1. **Durable local queue** (Room): generation prompts and curation edits
   (`like` / `skip` / rating notes) with status `PENDING` | `SYNCED` | `FAILED`.
2. **Local library playback**: scans app `files/library/` and SAF music folders;
   list tracks; play/pause via Media3 ExoPlayer + MediaSession (notification / lock screen / BT).
3. **UI shells**: Library, Queue, Settings (peer host + port).
4. **PC presence + sync drain**: on resume and periodically, `GET /health`
   (or `/v1/status`). When online, `POST /v1/jobs` for each pending item and
   mark synced. Exponential backoff while offline. Clear Logcat tags
   `BopSync` / `BopPc`.

WAN assumption: Tailscale Personal with MagicDNS names (e.g. `powerspec`) or
Tailscale IPs. **Tailscale is not required to scaffold or build** — configure
any reachable host in Settings.

## Run the PC stub

```bash
cd pc
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8765 --reload
```

See [pc/README.md](pc/README.md).

## Open the Android app

1. Open **Android Studio** → *Open* → select the `android/` directory
   (or the repo root if you prefer; the Gradle project lives under `android/`).
2. Let Gradle sync (SDK 34 / minSdk 26, Kotlin, Compose).
3. Run on an emulator or device.
4. In **Settings**, set peer host to `10.0.2.2` (emulator → host loopback),
   your LAN IP, or a MagicDNS name like `powerspec`, and port `8765`.

Drop sample audio into the app’s `files/library/` via Device File Explorer, or
use the in-app “Rescan library” after adding files under app storage.


## Verify queue drain (end-to-end)

1. On **powerspec**, start the stub:
   ```bash
   cd pc
   source .venv/bin/activate   # or: python -m venv .venv && pip install -r requirements.txt
   uvicorn main:app --host 0.0.0.0 --port 8765 --reload
   ```
2. On the phone app: **Settings** → host `powerspec` (or `100.112.170.62`), port `8765` → Save → confirm Presence **ONLINE**.
3. **Queue** → + → enter a prompt (e.g. `lofi rain at night`) → **Enqueue**.
4. Tap **Sync now** (or wait for the automatic drain).
5. **Expect**
   - uvicorn log: `POST /v1/jobs accepted id=… local_id=… prompt='lofi rain…'`
   - Queue row chip flips to **SYNCED** and shows `PC job <uuid>`
   - Banner shows `drained ok=1 fail=0 of 1 ids=…`
6. Optional: `curl http://powerspec:8765/v1/jobs` should list the accepted job.

Logcat filters: `BopSync`, `BopPc`.

## Assumptions

- Peer transport is plain HTTP (no TLS yet) on a private Tailscale/LAN network.
- Job store on the PC is in-memory for this slice.
- Android need not be compile-verified in CI here; structure matches current
  Android Gradle Plugin + Compose BOM conventions.
- No secrets in-repo; peer host/port live in DataStore preferences.

## License

MIT — see [LICENSE](LICENSE).\n
## Verify system media controls (Media3)

1. Install/rebuild, **Allow notifications** when prompted (Android 13+).
2. Library → play a track (in-app now-playing should work).
3. Pull down the shade / check lock screen: **Bop-Search / Now playing** media notification with title + play/pause.
4. Leave the app — audio continues; notification still controls it.
5. Optional: BT/headset buttons; unplug headphones should pause.

If the shade is empty but audio plays: open App info → Notifications and enable the **Now playing** channel.
