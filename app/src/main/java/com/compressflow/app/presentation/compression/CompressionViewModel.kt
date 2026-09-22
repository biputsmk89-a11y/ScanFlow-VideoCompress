package com.compressflow.app.presentation.compression

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.compressflow.app.data.session.CompressionSession
import com.compressflow.app.domain.model.CompressionResult
import com.compressflow.app.media.transformer.TransformerProcessor
import com.compressflow.app.media.validator.OutputValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

data class CompressionUiState(
    val jobId: String = "CF-${Random.nextInt(1000, 9999)}",
    val progress: Float = 0.0f,
    val currentPass: Int = 1,
    val totalPasses: Int = 1,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val estimatedResultBytes: Long = 0L,
    val remainingSeconds: Long = 0L,
    val framerate: Float = 30f,
    val bitrateKbps: Long = 2500L,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false,
    val error: String? = null,
    val vibrateOnComplete: Boolean = true
)

@OptIn(UnstableApi::class)
class CompressionViewModel(application: Application) : AndroidViewModel(application) {

    private val transformerProcessor = TransformerProcessor(application)
    private val outputValidator = OutputValidator(application)

    private val _uiState = MutableStateFlow(CompressionUiState())
    val uiState: StateFlow<CompressionUiState> = _uiState.asStateFlow()

    private var compressionJob: Job? = null
    private var startTimeMs: Long = 0L

    init {
        startCompression()
    }

    fun startCompression() {
        val uri = CompressionSession.currentUri
        val meta = CompressionSession.currentMetadata
        val plan = CompressionSession.currentPlan

        if (uri == null || meta == null || plan == null) {
            _uiState.value = _uiState.value.copy(error = "No active video session found")
            return
        }

        val app = getApplication<Application>()
        val outputFile = transformerProcessor.createOutputFile(app, meta.filename, plan.container)
        CompressionSession.outputFile = outputFile

        _uiState.value = _uiState.value.copy(
            totalBytes = meta.fileSize,
            estimatedResultBytes = plan.estimatedOutputSize,
            framerate = plan.targetFps,
            bitrateKbps = plan.targetVideoBitrate / 1000,
            remainingSeconds = (meta.duration / 1000 * 0.2).toLong().coerceAtLeast(5)
        )

        startTimeMs = System.currentTimeMillis()

        compressionJob = viewModelScope.launch {
            try {
                transformerProcessor.compress(
                    inputUri = uri,
                    outputFile = outputFile,
                    plan = plan,
                    originalSize = meta.fileSize
                ).collect { event ->
                    when (event) {
                        is TransformerProcessor.ProcessEvent.Progress -> {
                            val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000f
                            val fraction = event.fraction.coerceIn(0f, 0.99f)
                            val remaining = if (fraction > 0.05f) {
                                ((elapsedSec / fraction) * (1f - fraction)).toLong()
                            } else {
                                _uiState.value.remainingSeconds
                            }

                            _uiState.value = _uiState.value.copy(
                                progress = fraction,
                                processedBytes = (meta.fileSize * fraction).toLong(),
                                remainingSeconds = remaining.coerceAtLeast(1)
                            )
                        }

                        is TransformerProcessor.ProcessEvent.FallbackApplied -> {
                            android.util.Log.i(
                                "CompressionViewModel",
                                "Media3 Fallback applied: ${event.originalMimeType} -> ${event.fallbackMimeType}"
                            )
                        }

                        is TransformerProcessor.ProcessEvent.Complete -> {
                            val expectAudio = meta.hasAudio && !plan.removeAudio
                            val validation = outputValidator.validate(outputFile, expectAudio = expectAudio)
                            val finalResult = event.result.copy(
                                success = validation.isValid,
                                errorMessage = validation.errorMessage
                            )
                            CompressionSession.lastResult = finalResult

                            if (_uiState.value.vibrateOnComplete) {
                                triggerHaptic()
                            }

                            _uiState.value = _uiState.value.copy(
                                progress = 1f,
                                processedBytes = meta.fileSize,
                                remainingSeconds = 0L,
                                isCompleted = true
                            )
                        }

                        is TransformerProcessor.ProcessEvent.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = event.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Compression failed"
                )
            }
        }
    }

    fun toggleVibrate(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrateOnComplete = enabled)
    }

    fun cancelCompression() {
        compressionJob?.cancel()
        transformerProcessor.cancel()
        CompressionSession.outputFile?.delete()
        _uiState.value = _uiState.value.copy(isCancelled = true)
    }

    private fun triggerHaptic() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(150)
            }
        } catch (_: Exception) {}
    }
}
