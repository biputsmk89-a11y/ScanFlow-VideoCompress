package com.compressflow.app.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompressionHistoryDao {

    @Query("SELECT * FROM compression_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CompressionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CompressionHistoryEntity): Long

    @Delete
    suspend fun delete(item: CompressionHistoryEntity)

    @Query("DELETE FROM compression_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM compression_history")
    suspend fun clearAll()
}
