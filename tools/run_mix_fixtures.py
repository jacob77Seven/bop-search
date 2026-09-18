#!/usr/bin/env python3
"""Golden Mix evaluator — mirrors android domain MixEvaluator + fixtures/README.md."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "docs" / "schema" / "fixtures"


def score(rules: dict, track: dict) -> float:
    like_bias = float(rules.get("like_bias", 0))
    new_bias = float(rules.get("new_bias", 0))
    rating = float(track.get("rating", 50))
    play_count = int(track.get("play_count", 0))
    like = like_bias * (rating / 100.0)
    freshness = 1.0 - (min(play_count, 10) / 10.0)
    return like + new_bias * freshness


def matches(rules: dict, track: dict) -> bool:
    if track.get("blacklist"):
        return False
    genres = [g.lower() for g in track.get("genre_ids") or []]
    genre = rules.get("genre") or {}
    exclude = [g.lower() for g in genre.get("exclude") or []]
    require = [g.lower() for g in genre.get("require") or []]
    allow = [g.lower() for g in genre.get("allow") or []]
    if exclude and any(g in exclude for g in genres):
        return False
    if require and any(g not in genres for g in require):
        return False
    if allow and (not genres or not any(g in allow for g in genres)):
        return False
    energy = track.get("energy")
    er = rules.get("energy") or {}
    if energy is not None:
        if "min" in er and energy < er["min"]:
            return False
        if "max" in er and energy > er["max"]:
            return False
    tone = track.get("tone")
    tr = rules.get("tone") or {}
    if tone is not None:
        if "min" in tr and tone < tr["min"]:
            return False
        if "max" in tr and tone > tr["max"]:
            return False
    if "rating_min" in rules and track.get("rating", 50) < rules["rating_min"]:
        return False
    if "rating_max" in rules and track.get("rating", 50) > rules["rating_max"]:
        return False
    return True


def evaluate(mix: dict, tracks: list) -> list:
    rules = mix["rules"]
    filtered = [t for t in tracks if matches(rules, t)]
    filtered.sort(key=lambda t: (-score(rules, t), t["id"]))
    return [t["id"] for t in filtered]


def main() -> int:
    files = sorted(FIXTURES.glob("mix-*.json"))
    if not files:
        print("No fixtures found", file=sys.stderr)
        return 1
    failed = 0
    for path in files:
        data = json.loads(path.read_text())
        got = evaluate(data["mix"], data["tracks"])
        exp = data["expected_order"]
        ok = got == exp
        status = "PASS" if ok else "FAIL"
        print(f"{status}  {data['name']}  ({path.name})")
        if not ok:
            print(f"       expected {exp}")
            print(f"       got      {got}")
            failed += 1
    print(f"\n{len(files) - failed}/{len(files)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
