package com.jacob77.bopsearch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CurationEventDao {
    @Insert
    suspend fun insert(event: CurationEventEntity): Long

    @Query("SELECT * FROM curation_events ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<CurationEventEntity>>

    @Query("SELECT * FROM curation_events WHERE synced = 0 ORDER BY createdAtEpochMs ASC")
    suspend fun listUnsynced(): List<CurationEventEntity>

    @Query("UPDATE curation_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
