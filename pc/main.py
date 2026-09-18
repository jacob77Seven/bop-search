"""Bop-Search PC stub — accept queue drain from the Android client."""

from __future__ import annotations

import logging
import uuid
from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [bop-pc] %(message)s",
)
log = logging.getLogger("bop-search-pc")

app = FastAPI(title="bop-search-pc", version="0.1.0")

# In-memory store for this slice; swap for sqlite later.
_jobs: dict[str, dict[str, Any]] = {}


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


class JobCreate(BaseModel):
    prompt: str = Field(..., min_length=1)
    metadata: dict[str, Any] = Field(default_factory=dict)
    client_id: str = Field(default="android")
    local_id: str | None = Field(
        default=None,
        description="Room row id on the phone, for idempotent drain",
    )
    kind: str = Field(
        default="generate",
        description="generate | curation",
    )


class JobOut(BaseModel):
    id: str
    prompt: str
    metadata: dict[str, Any]
    client_id: str
    local_id: str | None
    kind: str
    status: str
    created_at: str
    updated_at: str


@app.get("/health")
def health() -> dict[str, Any]:
    return {"ok": True, "service": "bop-search-pc"}


@app.get("/v1/status")
def status() -> dict[str, Any]:
    return {
        "ok": True,
        "service": "bop-search-pc",
        "jobs": len(_jobs),
        "generation": "stub",
    }


@app.post("/v1/jobs", response_model=JobOut, status_code=201)
def create_job(body: JobCreate) -> JobOut:
    # Idempotent drain: reuse existing job for same client local_id.
    if body.local_id:
        for existing in _jobs.values():
            if (
                existing.get("client_id") == body.client_id
                and existing.get("local_id") == body.local_id
            ):
                log.info("idempotent hit local_id=%s -> %s", body.local_id, existing["id"])
                return JobOut(**existing)

    now = _utc_now()
    job_id = str(uuid.uuid4())
    record = {
        "id": job_id,
        "prompt": body.prompt,
        "metadata": body.metadata,
        "client_id": body.client_id,
        "local_id": body.local_id,
        "kind": body.kind,
        "status": "accepted",
        "created_at": now,
        "updated_at": now,
    }
    _jobs[job_id] = record
    log.info(
        "accepted job id=%s kind=%s client=%s local_id=%s prompt=%r",
        job_id,
        body.kind,
        body.client_id,
        body.local_id,
        body.prompt[:80],
    )
    return JobOut(**record)


@app.get("/v1/jobs", response_model=list[JobOut])
def list_jobs() -> list[JobOut]:
    ordered = sorted(_jobs.values(), key=lambda j: j["created_at"], reverse=True)
    return [JobOut(**j) for j in ordered]


@app.get("/v1/jobs/{job_id}", response_model=JobOut)
def get_job(job_id: str) -> JobOut:
    record = _jobs.get(job_id)
    if not record:
        raise HTTPException(status_code=404, detail="job not found")
    return JobOut(**record)
