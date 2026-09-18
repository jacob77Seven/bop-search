package com.jacob77.bopsearch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackMetaDao {
    @Query("SELECT * FROM track_meta")
    fun observeAll(): Flow<List<TrackMetaEntity>>

    @Query("SELECT * FROM track_meta WHERE trackId = :trackId LIMIT 1")
    suspend fun get(trackId: String): TrackMetaEntity?

    @Query("SELECT * FROM track_meta WHERE trackId = :trackId LIMIT 1")
    fun observe(trackId: String): Flow<TrackMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: TrackMetaEntity)

    @Query(
        """
        UPDATE track_meta SET
          rating = :rating,
          updatedAtEpochMs = :updatedAt
        WHERE trackId = :trackId
        """,
    )
    suspend fun updateRating(trackId: String, rating: Int, updatedAt: Long)

    @Query(
        """
        UPDATE track_meta SET
          playCount = playCount + 1,
          lastPlayedEpochMs = :playedAt,
          updatedAtEpochMs = :playedAt
        WHERE trackId = :trackId
        """,
    )
    suspend fun bumpPlay(trackId: String, playedAt: Long)
}
