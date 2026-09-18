# Bop-Search PC stub

Thin HTTP API that the Android app probes and drains its offline queue into.
Generation (ComfyUI / local models) is wired later — this slice only accepts
and stores jobs.

## Run locally

```bash
cd pc
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8765 --reload
```

Health check:

```bash
curl http://127.0.0.1:8765/health
```

On a Tailscale-connected machine, use MagicDNS (`powerspec`) or the Tailscale IP
from the Android Settings screen. The phone does not need Tailscale installed
to build/run — only a reachable HTTP peer host.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Liveness; Android presence probe |
| GET | `/v1/status` | Alias of health with a bit more detail |
| POST | `/v1/jobs` | Accept a queued generation/curation job |
| GET | `/v1/jobs` | List jobs (newest first) |
| GET | `/v1/jobs/{id}` | Job by id |

## Next (not in this slice)

- Persist jobs to SQLite
- Hand off prompts to ComfyUI / local generation
- Push completed audio back to the phone library
