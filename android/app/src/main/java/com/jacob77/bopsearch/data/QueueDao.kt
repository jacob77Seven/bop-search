package com.jacob77.bopsearch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<QueueItemEntity>>

    @Query("SELECT * FROM queue_items WHERE status = :status ORDER BY createdAtEpochMs ASC")
    suspend fun listByStatus(status: QueueStatus): List<QueueItemEntity>

    @Query("SELECT * FROM queue_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QueueItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QueueItemEntity): Long

    @Update
    suspend fun update(item: QueueItemEntity)

    @Query(
        """
        UPDATE queue_items SET
          status = :status,
          remoteJobId = :remoteJobId,
          lastError = :lastError,
          syncedAtEpochMs = :syncedAt,
          updatedAtEpochMs = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun markSyncResult(
        id: Long,
        status: QueueStatus,
        remoteJobId: String?,
        lastError: String?,
        syncedAt: Long?,
        updatedAt: Long,
    )

    @Query("DELETE FROM queue_items WHERE id = :id")
    suspend fun delete(id: Long)
}
