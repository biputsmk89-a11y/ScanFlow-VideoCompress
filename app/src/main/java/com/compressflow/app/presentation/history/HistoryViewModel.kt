package com.compressflow.app.presentation.history

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import com.compressflow.app.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class HistorySortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    LARGEST_SAVINGS("Highest Savings"),
    SMALLEST_SIZE("Smallest Size")
}

enum class HistoryFilterOption(val label: String) {
    ALL("All"),
    VIDEO("Video Only"),
    AUDIO("Audio Only")
}

data class HistoryStats(
    val totalCount: Int = 0,
    val totalSavedBytes: Long = 0L,
    val totalOutputBytes: Long = 0L,
    val avgSavedPercentage: Float = 0f
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(HistorySortOption.NEWEST)
    val sortOption: StateFlow<HistorySortOption> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(HistoryFilterOption.ALL)
    val filterOption: StateFlow<HistoryFilterOption> = _filterOption.asStateFlow()

    val historyList: StateFlow<List<CompressionHistoryEntity>> = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<HistoryStats> = historyList.combine(_searchQuery) { list, _ ->
        val count = list.size
        val savedBytes = list.sumOf { (it.originalSize - it.compressedSize).coerceAtLeast(0L) }
        val outputBytes = list.sumOf { it.compressedSize }
        val avgPct = if (count > 0) list.map { it.savedPercentage }.average().toFloat() else 0f
        HistoryStats(
            totalCount = count,
            totalSavedBytes = savedBytes,
            totalOutputBytes = outputBytes,
            avgSavedPercentage = avgPct
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryStats()
    )

    val filteredHistory: StateFlow<List<CompressionHistoryEntity>> = combine(
        historyList,
        _searchQuery,
        _sortOption,
        _filterOption
    ) { list, query, sort, filter ->
        var filtered = if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.filename.contains(query, ignoreCase = true) ||
                        it.resolution.contains(query, ignoreCase = true) ||
                        it.codec.contains(query, ignoreCase = true)
            }
        }

        filtered = when (filter) {
            HistoryFilterOption.ALL -> filtered
            HistoryFilterOption.VIDEO -> filtered.filter { !isAudioItem(it) }
            HistoryFilterOption.AUDIO -> filtered.filter { isAudioItem(it) }
        }

        when (sort) {
            HistorySortOption.NEWEST -> filtered.sortedByDescending { it.timestamp }
            HistorySortOption.OLDEST -> filtered.sortedBy { it.timestamp }
            HistorySortOption.LARGEST_SAVINGS -> filtered.sortedByDescending { it.savedPercentage }
            HistorySortOption.SMALLEST_SIZE -> filtered.sortedBy { it.compressedSize }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: HistorySortOption) {
        _sortOption.value = option
    }

    fun setFilterOption(option: HistoryFilterOption) {
        _filterOption.value = option
    }

    fun deleteItem(item: CompressionHistoryEntity, deleteFileFromStorage: Boolean = false) {
        viewModelScope.launch {
            if (deleteFileFromStorage) {
                try {
                    if (item.outputPath.startsWith("content://")) {
                        getApplication<Application>().contentResolver.delete(
                            Uri.parse(item.outputPath), null, null
                        )
                    } else {
                        val file = File(item.outputPath)
                        if (file.exists()) file.delete()
                    }
                } catch (_: Exception) {}
            }
            repository.deleteHistory(item)
        }
    }

    fun clearAll(deleteFilesFromStorage: Boolean = false) {
        viewModelScope.launch {
            if (deleteFilesFromStorage) {
                val currentItems = historyList.value
                currentItems.forEach { item ->
                    try {
                        if (item.outputPath.startsWith("content://")) {
                            getApplication<Application>().contentResolver.delete(
                                Uri.parse(item.outputPath), null, null
                            )
                        } else {
                            val file = File(item.outputPath)
                            if (file.exists()) file.delete()
                        }
                    } catch (_: Exception) {}
                }
            }
            repository.clearAll()
        }
    }

    fun playItem(item: CompressionHistoryEntity, onFileNotFound: () -> Unit = {}) {
        val app = getApplication<Application>()
        try {
            val isAudio = isAudioItem(item)
            val uri = if (item.outputPath.startsWith("content://")) {
                Uri.parse(item.outputPath)
            } else {
                val file = File(item.outputPath)
                if (!file.exists()) {
                    onFileNotFound()
                    return
                }
                FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    file
                )
            }
            val mimeType = if (isAudio) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        } catch (_: Exception) {
            onFileNotFound()
        }
    }

    fun shareItem(item: CompressionHistoryEntity, onFileNotFound: () -> Unit = {}) {
        val app = getApplication<Application>()
        try {
            val isAudio = isAudioItem(item)
            val uri = if (item.outputPath.startsWith("content://")) {
                Uri.parse(item.outputPath)
            } else {
                val file = File(item.outputPath)
                if (!file.exists()) {
                    onFileNotFound()
                    return
                }
                FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    file
                )
            }
            val mimeType = if (isAudio) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share output file").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        } catch (_: Exception) {
            onFileNotFound()
        }
    }

    // Backward-compatibility aliases for HomeScreen
    fun playVideo(item: CompressionHistoryEntity) = playItem(item)
    fun shareVideo(item: CompressionHistoryEntity) = shareItem(item)

    private fun isAudioItem(item: CompressionHistoryEntity): Boolean {
        return item.filename.endsWith(".m4a", ignoreCase = true) ||
                item.filename.endsWith(".mp3", ignoreCase = true) ||
                item.filename.endsWith(".aac", ignoreCase = true) ||
                item.resolution.contains("audio", ignoreCase = true) ||
                item.codec.contains("audio", ignoreCase = true) ||
                item.codec.contains("aac", ignoreCase = true)
    }
}
