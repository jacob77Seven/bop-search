package com.jacob77.bopsearch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MixDao {
    @Query("SELECT * FROM mixes ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<MixEntity>>

    @Query("SELECT * FROM mixes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MixEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mix: MixEntity): Long

    @Update
    suspend fun update(mix: MixEntity)

    @Query("DELETE FROM mixes WHERE id = :id")
    suspend fun delete(id: Long)
}
