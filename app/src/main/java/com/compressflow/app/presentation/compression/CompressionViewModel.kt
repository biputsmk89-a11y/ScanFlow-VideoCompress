package com.compressflow.app.presentation.compression

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import com.compressflow.app.data.repository.HistoryRepository
import com.compressflow.app.data.session.CompressionSession
import com.compressflow.app.domain.model.CompressionPreset
import com.compressflow.app.domain.model.CompressionResult
import com.compressflow.app.media.analyzer.VideoAnalyzer
import com.compressflow.app.media.capability.CapabilityDetector
import com.compressflow.app.media.planner.CompressionPlanner
import com.compressflow.app.media.transformer.TransformerProcessor
import com.compressflow.app.media.validator.OutputValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val historyRepository = HistoryRepository(application)
    private val settingsRepository = com.compressflow.app.data.preferences.SettingsRepository(application)

    private val _uiState = MutableStateFlow(CompressionUiState())
    val uiState: StateFlow<CompressionUiState> = _uiState.asStateFlow()

    private var compressionJob: Job? = null
    private var startTimeMs: Long = 0L

    init {
        startCompression()
    }

    fun startCompression() {
        val app = getApplication<Application>()
        val batchList = if (CompressionSession.batchUris.isNotEmpty()) {
            CompressionSession.batchUris
        } else {
            listOfNotNull(CompressionSession.currentUri)
        }

        if (batchList.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No active video session found")
            return
        }

        transformerProcessor.cleanOrphanedCacheFiles(app)

        val totalPasses = batchList.size
        _uiState.value = _uiState.value.copy(totalPasses = totalPasses)

        compressionJob = viewModelScope.launch {
            try {
                for (itemIndex in batchList.indices) {
                    val uri = batchList[itemIndex]
                    val meta = if (itemIndex == 0 && CompressionSession.currentMetadata != null) {
                        CompressionSession.currentMetadata!!
                    } else {
                        val analysis = VideoAnalyzer(app).analyze(uri)
                        if (analysis.isFailure) {
                            _uiState.value = _uiState.value.copy(
                                error = analysis.exceptionOrNull()?.message ?: "Unsupported video format: ${uri.lastPathSegment ?: "unknown"}"
                            )
                            return@launch
                        }
                        analysis.getOrNull()
                    }

                    if (meta == null) {
                        _uiState.value = _uiState.value.copy(
                            error = "Unable to process video: ${uri.lastPathSegment ?: "unknown"}. File may be corrupted."
                        )
                        return@launch
                    }

                    val appSettings = settingsRepository.settingsFlow.first()

                    val plan = if (itemIndex == 0 && CompressionSession.currentPlan != null) {
                        CompressionSession.currentPlan!!
                    } else {
                        val caps = CapabilityDetector().detect()
                        val preset = CompressionSession.currentPreset
                        val targetBytes = if (preset == CompressionPreset.TARGET_SIZE) {
                            CompressionSession.currentTargetSizeMb * 1024L * 1024L
                        } else null
                        var p = CompressionPlanner().plan(
                            metadata = meta,
                            preset = preset,
                            capabilities = caps,
                            targetSizeBytes = targetBytes
                        )
                        when (appSettings.defaultCodec) {
                            "H.265 / HEVC (Best compression)" -> if (caps.supportsH265) p = p.copy(videoCodec = com.compressflow.app.domain.model.VideoCodec.H265)
                            "H.264 (Maximum compatibility)" -> p = p.copy(videoCodec = com.compressflow.app.domain.model.VideoCodec.H264)
                            "AV1 (Next-gen)" -> if (caps.supportsAv1) p = p.copy(videoCodec = com.compressflow.app.domain.model.VideoCodec.AV1)
                        }
                        if (!appSettings.keepAudio) {
                            p = p.copy(removeAudio = true)
                        }
                        p
                    }

                    val outputFile = transformerProcessor.createOutputFile(
                        context = app,
                        originalFilename = meta.filename,
                        container = plan.container,
                        pattern = appSettings.filenamePattern
                    )
                    CompressionSession.outputFile = outputFile

                    _uiState.value = _uiState.value.copy(
                        currentPass = itemIndex + 1,
                        progress = 0f,
                        totalBytes = meta.fileSize,
                        estimatedResultBytes = plan.estimatedOutputSize,
                        framerate = plan.targetFps,
                        bitrateKbps = plan.targetVideoBitrate / 1000,
                        remainingSeconds = (meta.duration / 1000 * 0.2).toLong().coerceAtLeast(5)
                    )

                    startTimeMs = System.currentTimeMillis()

                    var itemCompleted = false
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
                                CompressionSession.batchResults.add(finalResult)
                                CompressionSession.lastResult = finalResult
                                itemCompleted = true

                                if (validation.isValid) {
                                    try {
                                        historyRepository.insertHistory(
                                            CompressionHistoryEntity(
                                                filename = meta.filename ?: "video.mp4",
                                                originalSize = meta.fileSize,
                                                compressedSize = finalResult.outputSize,
                                                savedPercentage = finalResult.savedPercentage,
                                                durationMs = meta.duration,
                                                resolution = "${plan.targetWidth}×${plan.targetHeight}",
                                                codec = plan.videoCodec.displayName,
                                                outputPath = outputFile.absolutePath
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }

                                if (validation.isValid && appSettings.notifications) {
                                    showNotification(app, meta.filename ?: "video.mp4", finalResult.savedPercentage.toInt())
                                }

                                _uiState.value = _uiState.value.copy(
                                    progress = 1f,
                                    processedBytes = meta.fileSize,
                                    remainingSeconds = 0L
                                )
                            }

                            is TransformerProcessor.ProcessEvent.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    error = event.message
                                )
                            }
                        }
                    }

                    if (!itemCompleted && _uiState.value.isCancelled) {
                        break
                    }
                }

                if (_uiState.value.vibrateOnComplete) {
                    triggerHaptic()
                }

                _uiState.value = _uiState.value.copy(
                    progress = 1f,
                    remainingSeconds = 0L,
                    isCompleted = true
                )
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
        transformerProcessor.cleanOrphanedCacheFiles(getApplication())
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

    private fun showNotification(context: Context, filename: String, savedPercentage: Int) {
        try {
            val channelId = "compression_status_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Compression Status",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Shows notification when video compression completes"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, com.compressflow.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Compression Completed")
                .setContentText("$filename finished! Saved $savedPercentage% storage.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(2001, notification)
        } catch (_: Exception) {}
    }
}
