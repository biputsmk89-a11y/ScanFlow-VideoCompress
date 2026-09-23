package com.compressflow.app.data.repository

import android.content.Context
import com.compressflow.app.data.local.database.AppDatabase
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).historyDao()

    val allHistory: Flow<List<CompressionHistoryEntity>> = dao.getAllHistory()

    suspend fun insertHistory(item: CompressionHistoryEntity): Long {
        return dao.insert(item)
    }

    suspend fun deleteHistory(item: CompressionHistoryEntity) {
        dao.delete(item)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
