package com.jacob77.bopsearch.domain

/**
 * Mix document aligned with docs/schema/mix.schema.json.
 * Local Room may use a Long row id; [id] is the stable string identity.
 */
data class MixDocument(
    val id: String,
    val name: String,
    val icon: String? = null,
    val rules: MixRules,
)

/**
 * Mix rules object from mix.schema.json.
 * Energy/tone are 0–1 windows; rating is 0–100.
 */
data class MixRules(
    val genreAllow: List<String> = emptyList(),
    val genreRequire: List<String> = emptyList(),
    val genreExclude: List<String> = emptyList(),
    val toneMin: Double? = null,
    val toneMax: Double? = null,
    val energyMin: Double? = null,
    val energyMax: Double? = null,
    val ratingMin: Double? = null,
    val ratingMax: Double? = null,
    /** 0 = ignore rating; 1 = strongly prefer high-rated. */
    val likeBias: Double = 0.5,
    /** 0 = ignore newness; 1 = strongly prefer low play_count. */
    val newBias: Double = 0.5,
    val seed: Long? = null,
)

/**
 * Track fields needed for Mix evaluation (subset of track.schema.json + library path).
 */
data class MixCandidate(
    val id: String,
    val title: String = "",
    val path: String = "",
    val sizeBytes: Long = 0,
    val sourceLabel: String = "",
    val rating: Double = CurationRules.DEFAULT_RATING.toDouble(),
    val playCount: Int = 0,
    val dateAddedIso: String = "",
    val genreIds: List<String> = emptyList(),
    /** 0–1 per track.schema.json; null = untagged (passes energy filter). */
    val tone: Double? = null,
    val energy: Double? = null,
    val blacklist: Boolean = false,
)
