package com.jacob77.bopsearch.domain

/**
 * Shared curation rules (DESIGN.md): rating 0–100, default mid 50.
 * Skip lowers; full listen raises. Keep pure for dual Kotlin/Python later.
 */
object CurationRules {
    const val DEFAULT_RATING = 50
    const val MIN_RATING = 0
    const val MAX_RATING = 100
    const val SKIP_DELTA = -5
    const val FULL_LISTEN_DELTA = 5
    /** Below this → blacklist candidate (PC may trash later). */
    const val BLACKLIST_THRESHOLD = 15

    fun clamp(rating: Int): Int = rating.coerceIn(MIN_RATING, MAX_RATING)

    fun applySkip(current: Int?): Int = clamp((current ?: DEFAULT_RATING) + SKIP_DELTA)

    fun applyFullListen(current: Int?): Int = clamp((current ?: DEFAULT_RATING) + FULL_LISTEN_DELTA)

    fun resolve(current: Int?): Int = current ?: DEFAULT_RATING
}
