package com.compressflow.app.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compression_history")
data class CompressionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filename: String,
    val originalSize: Long,
    val compressedSize: Long,
    val savedPercentage: Float,
    val durationMs: Long,
    val resolution: String,
    val codec: String,
    val outputPath: String,
    val timestamp: Long = System.currentTimeMillis()
)
