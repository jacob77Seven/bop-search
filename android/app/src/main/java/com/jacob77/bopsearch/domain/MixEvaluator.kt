package com.jacob77.bopsearch.domain

import kotlin.math.min

/**
 * Offline Mix evaluation locked to docs/schema/fixtures/README.md:
 * 1. Filter blacklist
 * 2. Genre allow/require/exclude (exact id match)
 * 3. Tone / energy / rating ranges when present
 * 4. Score = like_bias*(rating/100) + new_bias*(1 - min(play_count,10)/10)
 * 5. Sort score desc, tie-break id asc
 * 6. seed ignored in P0
 */
object MixEvaluator {

    fun evaluate(
        rules: MixRules,
        candidates: List<MixCandidate>,
        limit: Int = Int.MAX_VALUE,
    ): List<MixCandidate> {
        val filtered = candidates.filter { matches(rules, it) }
        return filtered
            .sortedWith(
                compareByDescending<MixCandidate> { score(rules, it) }
                    .thenBy { it.id },
            )
            .take(limit)
    }

    fun evaluateIds(rules: MixRules, candidates: List<MixCandidate>, limit: Int = Int.MAX_VALUE): List<String> =
        evaluate(rules, candidates, limit).map { it.id }

    fun matches(rules: MixRules, c: MixCandidate): Boolean {
        if (c.blacklist) return false

        val genres = c.genreIds.map { it.lowercase() }
        if (rules.genreExclude.isNotEmpty()) {
            val exclude = rules.genreExclude.map { it.lowercase() }
            if (genres.any { it in exclude }) return false
        }
        if (rules.genreRequire.isNotEmpty()) {
            val need = rules.genreRequire.map { it.lowercase() }
            if (need.any { it !in genres }) return false
        }
        if (rules.genreAllow.isNotEmpty()) {
            val allow = rules.genreAllow.map { it.lowercase() }
            // Untagged tracks fail allow-list (fixtures use explicit ids).
            if (genres.isEmpty() || genres.none { it in allow }) return false
        }

        val tone = c.tone
        if (tone != null) {
            rules.toneMin?.let { if (tone < it) return false }
            rules.toneMax?.let { if (tone > it) return false }
        } else if (rules.toneMin != null || rules.toneMax != null) {
            // Untagged: pass (same as energy when only some tracks tagged in fixtures).
        }

        val energy = c.energy
        if (energy != null) {
            rules.energyMin?.let { if (energy < it) return false }
            rules.energyMax?.let { if (energy > it) return false }
        }

        rules.ratingMin?.let { if (c.rating < it) return false }
        rules.ratingMax?.let { if (c.rating > it) return false }

        return true
    }

    /**
     * Fixture formula:
     * like_bias * (rating/100) + new_bias * (1 - min(play_count,10)/10)
     */
    fun score(rules: MixRules, c: MixCandidate): Double {
        val like = rules.likeBias.coerceIn(0.0, 1.0) * (c.rating / 100.0)
        val freshness = 1.0 - (min(c.playCount, 10) / 10.0)
        val neu = rules.newBias.coerceIn(0.0, 1.0) * freshness
        return like + neu
    }
}
