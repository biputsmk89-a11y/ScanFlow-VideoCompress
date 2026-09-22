package com.compressflow.app.presentation.result

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compressflow.app.data.session.CompressionSession
import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.CompressionResult
import com.compressflow.app.domain.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ResultUiState(
    val result: CompressionResult? = null,
    val metadata: VideoMetadata? = null,
    val plan: CompressionPlan? = null,
    val outputFile: File? = null,
    val ssimScore: Int = 96,
    val splitRatio: Float = 0.5f,
    val isOriginalDeleted: Boolean = false,
    val isSavedToGallery: Boolean = false,
    val message: String? = null
)

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        _uiState.value = ResultUiState(
            result = CompressionSession.lastResult,
            metadata = CompressionSession.currentMetadata,
            plan = CompressionSession.currentPlan,
            outputFile = CompressionSession.outputFile
        )
    }

    fun updateSplitRatio(ratio: Float) {
        _uiState.value = _uiState.value.copy(splitRatio = ratio.coerceIn(0f, 1f))
    }

    fun shareVideo(onStartActivity: (Intent) -> Unit) {
        val file = _uiState.value.outputFile ?: return
        val app = getApplication<Application>()
        try {
            val contentUri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onStartActivity(Intent.createChooser(shareIntent, "Share Compressed Video"))
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
            }
            onStartActivity(Intent.createChooser(shareIntent, "Share Compressed Video"))
        }
    }

    fun saveToGallery(onComplete: (String) -> Unit) {
        val file = _uiState.value.outputFile
        if (file == null || !file.exists()) {
            onComplete("No output file to save")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            try {
                val resolver = app.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CompressFlow")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isSavedToGallery = true)
                        onComplete("Saved to Movies/CompressFlow")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete("Failed to create gallery item")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete("Save failed: ${e.message}")
                }
            }
        }
    }

    fun deleteOriginalVideo() {
        viewModelScope.launch {
            val uri = CompressionSession.currentUri ?: return@launch
            val app = getApplication<Application>()
            try {
                app.contentResolver.delete(uri, null, null)
                _uiState.value = _uiState.value.copy(
                    isOriginalDeleted = true,
                    message = "Original video deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Could not delete original: ${e.message}"
                )
            }
        }
    }
}
