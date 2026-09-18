package com.jacob77.bopsearch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayQueueDao {
    @Query("SELECT * FROM play_queue ORDER BY position ASC")
    fun observeAll(): Flow<List<PlayQueueItemEntity>>

    @Query("SELECT * FROM play_queue ORDER BY position ASC")
    suspend fun listAll(): List<PlayQueueItemEntity>

    @Query("DELETE FROM play_queue")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlayQueueItemEntity>)

    @Transaction
    suspend fun replaceAll(items: List<PlayQueueItemEntity>) {
        clear()
        if (items.isNotEmpty()) insertAll(items)
    }

    @Query("DELETE FROM play_queue WHERE id = :id")
    suspend fun delete(id: Long)
}
