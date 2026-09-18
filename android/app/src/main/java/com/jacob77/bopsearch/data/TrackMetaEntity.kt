package com.jacob77.bopsearch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jacob77.bopsearch.domain.CurationRules

/** Per-track curation / tags keyed by LocalLibrary track id (track.schema subset). */
@Entity(tableName = "track_meta")
data class TrackMetaEntity(
    @PrimaryKey val trackId: String,
    val title: String = "",
    val path: String = "",
    val rating: Int = CurationRules.DEFAULT_RATING,
    val playCount: Int = 0,
    val lastPlayedEpochMs: Long? = null,
    val dateAddedEpochMs: Long = System.currentTimeMillis(),
    /** Comma-separated genre tree node ids. */
    val genreIds: String = "",
    /** 0–1 per track.schema.json */
    val tone: Double? = null,
    val energy: Double? = null,
    val blacklisted: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
