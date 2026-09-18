package com.jacob77.bopsearch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Ordered play queue (Queues tab). Distinct from Generate job queue. */
@Entity(tableName = "play_queue")
data class PlayQueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val position: Int,
    val trackId: String,
    val title: String,
    val path: String,
    val sizeBytes: Long = 0,
    val sourceLabel: String = "",
)
