# Mix evaluation golden fixtures

Each fixture is `{ "name", "mix", "tracks", "expected_order" }`.

Evaluator sketch (P0):
1. Filter out `blacklist`.
2. Apply genre allow/require/exclude (ids are exact match in fixtures).
3. Apply tone/energy/rating ranges when present.
4. Score remaining: `like_bias * (rating/100) + new_bias * (1 - min(play_count,10)/10)`.
5. Sort by score descending; break ties by `id` ascending.
6. `seed` reserved for later shuffle; ignore in P0 fixtures unless noted.
