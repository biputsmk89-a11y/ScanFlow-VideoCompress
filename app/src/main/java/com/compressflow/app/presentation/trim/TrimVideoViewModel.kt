package com.compressflow.app.presentation.trim

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.compressflow.app.domain.model.VideoMetadata
import com.compressflow.app.domain.usecase.TrimEvent
import com.compressflow.app.domain.usecase.TrimExecutionResult
import com.compressflow.app.domain.usecase.TrimVideoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * State of the Trim Video screen.
 */
data class TrimUiState(
    val videoUri: Uri? = null,
    val metadata: VideoMetadata? = null,
    val isLoading: Boolean = false,
    val durationMs: Long = 0L,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isPreviewingSelection: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val compressAlso: Boolean = false,
    val result: TrimExecutionResult? = null,
    val error: String? = null
) {
    val selectedDurationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

@OptIn(UnstableApi::class)
class TrimVideoViewModel(application: Application) : AndroidViewModel(application) {

    private val trimVideoUseCase = TrimVideoUseCase(application)

    private val _uiState = MutableStateFlow(TrimUiState())
    val uiState: StateFlow<TrimUiState> = _uiState.asStateFlow()

    var player: ExoPlayer? = null
        private set

    private var positionPollJob: Job? = null
    private var processingJob: Job? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        val app = getApplication<Application>()
        player = ExoPlayer.Builder(app).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) {
                        startPositionPolling()
                    } else {
                        stopPositionPolling()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _uiState.update {
                            it.copy(isPlaying = false, isPreviewingSelection = false)
                        }
                    }
                }
            })
        }
    }

    fun loadVideo(uri: Uri) {
        _uiState.update { it.copy(isLoading = true, videoUri = uri, error = null) }
        viewModelScope.launch {
            val result = trimVideoUseCase.analyzeVideo(uri)
            if (result.isSuccess) {
                val meta = result.getOrThrow()
                val duration = meta.duration.coerceAtLeast(1000L)

                player?.apply {
                    setMediaItem(MediaItem.fromUri(uri))
                    prepare()
                    pause()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        videoUri = uri,
                        metadata = meta,
                        durationMs = duration,
                        startMs = 0L,
                        endMs = duration,
                        currentPositionMs = 0L,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Gagal membaca file video"
                    )
                }
            }
        }
    }

    fun setStartMs(ms: Long) {
        val duration = _uiState.value.durationMs
        val minClip = 1000L.coerceAtMost(duration)
        val maxStart = (_uiState.value.endMs - minClip).coerceAtLeast(0L)
        val clampedStart = ms.coerceIn(0L, maxStart)

        _uiState.update { it.copy(startMs = clampedStart) }
        player?.seekTo(clampedStart)
        _uiState.update { it.copy(currentPositionMs = clampedStart) }
    }

    fun setEndMs(ms: Long) {
        val duration = _uiState.value.durationMs
        val minClip = 1000L.coerceAtMost(duration)
        val minEnd = (_uiState.value.startMs + minClip).coerceAtMost(duration)
        val clampedEnd = ms.coerceIn(minEnd, duration)

        _uiState.update { it.copy(endMs = clampedEnd) }
        player?.seekTo(clampedEnd)
        _uiState.update { it.copy(currentPositionMs = clampedEnd) }
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            _uiState.update { it.copy(isPreviewingSelection = false) }
        } else {
            // If current position is at or beyond end, seek back to start
            val currentPos = p.currentPosition
            if (currentPos >= _uiState.value.endMs || currentPos < _uiState.value.startMs) {
                p.seekTo(_uiState.value.startMs)
            }
            p.play()
        }
    }

    fun previewSelection() {
        val p = player ?: return
        p.seekTo(_uiState.value.startMs)
        _uiState.update { it.copy(isPreviewingSelection = true) }
        p.play()
    }

    fun seekTo(positionMs: Long) {
        val duration = _uiState.value.durationMs
        val clamped = positionMs.coerceIn(0L, duration)
        player?.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
    }

    fun setCompressAlso(enabled: Boolean) {
        _uiState.update { it.copy(compressAlso = enabled) }
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = viewModelScope.launch {
            while (isActive) {
                val current = player?.currentPosition ?: 0L
                _uiState.update { it.copy(currentPositionMs = current) }

                // If previewing selection and reached end boundary, pause and reset to start
                if (_uiState.value.isPreviewingSelection && current >= _uiState.value.endMs) {
                    player?.pause()
                    player?.seekTo(_uiState.value.startMs)
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            isPreviewingSelection = false,
                            currentPositionMs = it.startMs
                        )
                    }
                    break
                }
                delay(80L)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = null
        val current = player?.currentPosition ?: 0L
        _uiState.update { it.copy(currentPositionMs = current) }
    }

    fun startTrim() {
        val uri = _uiState.value.videoUri ?: return
        val start = _uiState.value.startMs
        val end = _uiState.value.endMs
        val compressAlso = _uiState.value.compressAlso

        player?.pause()
        _uiState.update { it.copy(isProcessing = true, progress = 0f, error = null) }

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            trimVideoUseCase.execute(
                inputUri = uri,
                startMs = start,
                endMs = end,
                compressAlso = compressAlso
            ).collect { event ->
                when (event) {
                    is TrimEvent.Progress -> {
                        _uiState.update { it.copy(progress = event.fraction) }
                    }

                    is TrimEvent.Complete -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                progress = 1f,
                                result = event.result
                            )
                        }
                    }

                    is TrimEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                error = event.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelTrim() {
        processingJob?.cancel()
        trimVideoUseCase.cancel()
        _uiState.update { it.copy(isProcessing = false, progress = 0f) }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun playOutput(outputPath: String) {
        val app = getApplication<Application>()
        try {
            val uri = if (outputPath.startsWith("content://")) {
                Uri.parse(outputPath)
            } else {
                val file = File(outputPath)
                if (!file.exists()) return
                FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun shareOutput(outputPath: String) {
        val app = getApplication<Application>()
        try {
            val uri = if (outputPath.startsWith("content://")) {
                Uri.parse(outputPath)
            } else {
                val file = File(outputPath)
                if (!file.exists()) return
                FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share trimmed video").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        positionPollJob?.cancel()
        processingJob?.cancel()
        player?.release()
        player = null
    }
}
