package com.jacob77.bopsearch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "curation_events")
data class CurationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    /** skip | full_listen | manual */
    val action: String,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    /** Pending sync to PC when online. */
    val synced: Boolean = false,
)
