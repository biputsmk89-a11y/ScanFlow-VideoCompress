package com.compressflow.app.presentation.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import com.compressflow.app.data.repository.HistoryRepository
import com.compressflow.app.data.session.CompressionSession
import com.compressflow.app.domain.model.CompressionPreset
import com.compressflow.app.media.capability.CapabilityDetector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository(application)
    val capabilities = CapabilityDetector().detect()

    // Observe latest 3 compression history items
    val recentHistory: StateFlow<List<CompressionHistoryEntity>> = historyRepository.allHistory
        .map { list -> list.take(3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onPresetSelected(preset: CompressionPreset) {
        CompressionSession.currentPreset = preset
    }

    fun playVideo(item: CompressionHistoryEntity) {
        val app = getApplication<Application>()
        try {
            val isAudio = item.filename.endsWith(".m4a", ignoreCase = true) ||
                    item.filename.endsWith(".mp3", ignoreCase = true) ||
                    item.codec.contains("audio", ignoreCase = true)
            val uri = if (item.outputPath.startsWith("content://")) {
                Uri.parse(item.outputPath)
            } else {
                val file = File(item.outputPath)
                if (!file.exists()) {
                    android.widget.Toast.makeText(app, "File tidak ditemukan di penyimpanan", android.widget.Toast.LENGTH_SHORT).show()
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
            android.widget.Toast.makeText(app, "Tidak dapat memutar file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
