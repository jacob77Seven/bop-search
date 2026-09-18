package com.jacob77.bopsearch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String, // "generate" | "curation"
    val prompt: String,
    /** Optional curation payload: like/skip/rating notes as JSON-ish text. */
    val curationNotes: String = "",
    val rating: Int? = null, // 1–5 or null
    val curationAction: String? = null, // like | skip | rate | null
    val status: QueueStatus = QueueStatus.PENDING,
    val remoteJobId: String? = null,
    val lastError: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncedAtEpochMs: Long? = null,
)
