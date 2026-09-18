package com.jacob77.bopsearch.domain

import java.io.InputStreamReader
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Golden fixtures from docs/schema/fixtures/ (copied to test resources).
 * Algorithm locked by docs/schema/fixtures/README.md.
 */
@RunWith(Parameterized::class)
class MixEvaluatorFixtureTest(private val fixtureName: String) {

    @Test
    fun evaluateMatchesExpectedOrder() {
        val root = loadFixture(fixtureName)
        val mix = root.getJSONObject("mix")
        val rulesJson = mix.getJSONObject("rules")
        val rules = parseRules(rulesJson)
        val tracks = parseTracks(root.getJSONArray("tracks"))
        val expected = jsonStringList(root.getJSONArray("expected_order"))
        val got = MixEvaluator.evaluateIds(rules, tracks)
        assertEquals(root.getString("name"), expected, got)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fixtures(): List<String> = listOf(
            "mix-rating-min.json",
            "mix-like-bias.json",
            "mix-new-bias.json",
            "mix-genre-exclude.json",
            "mix-energy-window.json",
        )

        private fun loadFixture(name: String): JSONObject {
            val stream = MixEvaluatorFixtureTest::class.java.classLoader!!
                .getResourceAsStream("fixtures/$name")
                ?: error("Missing fixture resource fixtures/$name")
            return JSONObject(InputStreamReader(stream).readText())
        }

        private fun parseRules(o: JSONObject): MixRules {
            val genre = o.optJSONObject("genre")
            val tone = o.optJSONObject("tone")
            val energy = o.optJSONObject("energy")
            return MixRules(
                genreAllow = jsonStringList(genre?.optJSONArray("allow")),
                genreRequire = jsonStringList(genre?.optJSONArray("require")),
                genreExclude = jsonStringList(genre?.optJSONArray("exclude")),
                toneMin = tone?.optDoubleOrNull("min"),
                toneMax = tone?.optDoubleOrNull("max"),
                energyMin = energy?.optDoubleOrNull("min"),
                energyMax = energy?.optDoubleOrNull("max"),
                ratingMin = o.optDoubleOrNull("rating_min"),
                ratingMax = o.optDoubleOrNull("rating_max"),
                likeBias = o.getDouble("like_bias"),
                newBias = o.getDouble("new_bias"),
                seed = if (o.has("seed") && !o.isNull("seed")) o.getLong("seed") else null,
            )
        }

        private fun parseTracks(arr: JSONArray): List<MixCandidate> {
            val out = ArrayList<MixCandidate>(arr.length())
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                out += MixCandidate(
                    id = t.getString("id"),
                    rating = t.getDouble("rating"),
                    playCount = t.optInt("play_count", 0),
                    dateAddedIso = t.optString("date_added", ""),
                    genreIds = jsonStringList(t.optJSONArray("genre_ids")),
                    tone = t.optDoubleOrNull("tone"),
                    energy = t.optDoubleOrNull("energy"),
                    blacklist = t.optBoolean("blacklist", false),
                )
            }
            return out
        }

        private fun jsonStringList(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }

        private fun JSONObject.optDoubleOrNull(key: String): Double? =
            if (has(key) && !isNull(key)) getDouble(key) else null
    }
}
