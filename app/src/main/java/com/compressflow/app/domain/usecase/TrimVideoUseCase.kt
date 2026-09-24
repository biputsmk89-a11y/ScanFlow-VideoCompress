package com.compressflow.app.domain.usecase

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import com.compressflow.app.data.repository.HistoryRepository
import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.OutputContainer
import com.compressflow.app.domain.model.VideoCodec
import com.compressflow.app.domain.model.VideoMetadata
import com.compressflow.app.media.analyzer.VideoAnalyzer
import com.compressflow.app.media.capability.CapabilityDetector
import com.compressflow.app.media.planner.CompressionPlanner
import com.compressflow.app.media.transformer.TransformerProcessor
import com.compressflow.app.media.validator.OutputValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/**
 * Result of a trim operation with verified metrics.
 */
data class TrimExecutionResult(
    val success: Boolean,
    val outputFile: File,
    val outputUri: String,
    val originalSize: Long,
    val outputSize: Long,
    val originalDurationMs: Long,
    val trimmedDurationMs: Long,
    val startMs: Long,
    val endMs: Long,
    val resolution: String,
    val codec: String,
    val errorMessage: String? = null
)

/**
 * Events emitted during trim execution.
 */
sealed class TrimEvent {
    data class Progress(val fraction: Float) : TrimEvent()
    data class Complete(val result: TrimExecutionResult) : TrimEvent()
    data class Error(val message: String) : TrimEvent()
}

/**
 * Use case orchestrating the Trim Video pipeline:
 * Input Video → Validation → Plan (Trim / Trim+Compress) → Transformer Engine
 * → File Writing → Output Validation → MediaStore/History Recording → Result
 */
@OptIn(UnstableApi::class)
class TrimVideoUseCase(private val context: Context) {

    private val videoAnalyzer = VideoAnalyzer(context)
    private val transformerProcessor = TransformerProcessor(context)
    private val outputValidator = OutputValidator(context)
    private val historyRepository = HistoryRepository(context)
    private val capabilityDetector = CapabilityDetector()

    fun cancel() {
        transformerProcessor.cancel()
    }

    suspend fun analyzeVideo(uri: Uri): Result<VideoMetadata> {
        return videoAnalyzer.analyze(uri)
    }

    /**
     * Executes the trim operation.
     *
     * @param inputUri Source video URI.
     * @param startMs Clip start position in milliseconds.
     * @param endMs Clip end position in milliseconds.
     * @param compressAlso If true, applies smart compression in the same pass.
     * @param targetCodec Optional codec override (H264, H265, AV1).
     */
    fun execute(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        compressAlso: Boolean = false,
        targetCodec: VideoCodec? = null
    ): Flow<TrimEvent> = callbackFlow {
        val safeStartMs = startMs.coerceAtLeast(0L)
        val expectedDurationMs = endMs - safeStartMs

        // Input range validation
        if (expectedDurationMs < 500L) {
            trySend(TrimEvent.Error("Selected clip must be at least 0.5 seconds long"))
            close()
            return@callbackFlow
        }

        // 1. Analyze input video
        val metaResult = videoAnalyzer.analyze(inputUri)
        if (metaResult.isFailure) {
            trySend(TrimEvent.Error(metaResult.exceptionOrNull()?.message ?: "Failed to read video"))
            close()
            return@callbackFlow
        }

        val meta = metaResult.getOrThrow()
        val caps = capabilityDetector.detect()

        val safeEndMs = if (meta.duration > 0) {
            endMs.coerceAtMost(meta.duration)
        } else endMs

        val clipDuration = (safeEndMs - safeStartMs).coerceAtLeast(500L)

        // 2. Prepare compression/trim plan
        val plan = if (compressAlso) {
            val basePlan = CompressionPlanner().plan(
                metadata = meta,
                preset = com.compressflow.app.domain.model.CompressionPreset.STORAGE_SAVER,
                capabilities = caps
            )
            basePlan.copy(
                trimStartMs = safeStartMs,
                trimEndMs = safeEndMs,
                videoCodec = targetCodec ?: basePlan.videoCodec
            )
        } else {
            // Trim only: preserve original visual dimensions, fps, bitrate, codec
            val codec = targetCodec ?: detectCodec(meta, caps)
            CompressionPlan(
                targetWidth = meta.width,
                targetHeight = meta.height,
                targetFps = meta.fps.takeIf { it > 0 } ?: 30f,
                targetVideoBitrate = meta.videoBitrate.takeIf { it > 0 } ?: 6_000_000L,
                targetAudioBitrate = meta.audioBitrate.takeIf { it > 0 } ?: 128_000L,
                videoCodec = codec,
                container = OutputContainer.MP4,
                removeAudio = false,
                estimatedOutputSize = 0L,
                trimStartMs = safeStartMs,
                trimEndMs = safeEndMs
            )
        }

        // 3. Create unique output file in cache
        val baseName = meta.filename?.substringBeforeLast(".")?.take(50) ?: "video"
        val timestamp = System.currentTimeMillis()
        val outputDir = File(context.cacheDir, "trimmed").apply { mkdirs() }
        val outputFile = File(outputDir, "${baseName}_trimmed_$timestamp.mp4")
        val displayName = "${baseName}_trimmed_$timestamp.mp4"

        // 4. Run Media3 Transformer with ClippingConfiguration
        transformerProcessor.compress(
            inputUri = inputUri,
            outputFile = outputFile,
            plan = plan,
            originalSize = meta.fileSize
        ).collect { event ->
            when (event) {
                is TransformerProcessor.ProcessEvent.Progress -> {
                    trySend(TrimEvent.Progress(event.fraction.coerceIn(0f, 0.99f)))
                }

                is TransformerProcessor.ProcessEvent.Complete -> {
                    // 5. Output Validation
                    val validation = outputValidator.validate(
                        outputFile = outputFile,
                        expectAudio = meta.hasAudio,
                        expectedDurationMs = clipDuration,
                        toleranceMs = 2500L // 2.5s tolerance for GOP boundaries
                    )

                    if (!validation.isValid) {
                        trySend(TrimEvent.Error(validation.errorMessage ?: "Output video validation failed"))
                        close()
                        return@collect
                    }

                    val finalOutputSize = outputFile.length()
                    val actualDuration = if (validation.actualDurationMs > 0) {
                        validation.actualDurationMs
                    } else clipDuration

                    // 6. Copy to public MediaStore (Movies/CompressFlow)
                    val savedUri = copyToPublicMovies(outputFile, displayName)
                    val finalPath = savedUri ?: outputFile.absolutePath

                    // 7. Record in Room History as TRIM
                    try {
                        val savedPct = if (meta.fileSize > 0) {
                            ((meta.fileSize - finalOutputSize).toFloat() / meta.fileSize * 100f).coerceAtLeast(0f)
                        } else 0f

                        historyRepository.insertHistory(
                            CompressionHistoryEntity(
                                filename = displayName,
                                originalSize = meta.fileSize,
                                compressedSize = finalOutputSize,
                                savedPercentage = savedPct,
                                durationMs = actualDuration,
                                resolution = "${meta.width}×${meta.height}",
                                codec = plan.videoCodec.displayName,
                                outputPath = finalPath
                            )
                        )
                    } catch (_: Exception) {}

                    val trimResult = TrimExecutionResult(
                        success = true,
                        outputFile = outputFile,
                        outputUri = finalPath,
                        originalSize = meta.fileSize,
                        outputSize = finalOutputSize,
                        originalDurationMs = meta.duration,
                        trimmedDurationMs = actualDuration,
                        startMs = safeStartMs,
                        endMs = safeEndMs,
                        resolution = "${meta.width}×${meta.height}",
                        codec = plan.videoCodec.displayName
                    )

                    trySend(TrimEvent.Complete(trimResult))
                    close()
                }

                is TransformerProcessor.ProcessEvent.Error -> {
                    trySend(TrimEvent.Error(event.message))
                    close()
                }

                is TransformerProcessor.ProcessEvent.FallbackApplied -> {
                    // Handled internally by transformer
                }
            }
        }
    }

    private fun detectCodec(meta: VideoMetadata, caps: CapabilityDetector.DeviceCapabilities): VideoCodec {
        return when {
            meta.videoCodec?.contains("hevc", true) == true && caps.supportsH265 -> VideoCodec.H265
            meta.videoCodec?.contains("av01", true) == true && caps.supportsAv1 -> VideoCodec.AV1
            else -> VideoCodec.H264
        }
    }

    @Suppress("DEPRECATION")
    private fun copyToPublicMovies(sourceFile: File, displayName: String): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/CompressFlow")
                }
                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = context.contentResolver.insert(collection, contentValues) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                return uri.toString()
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CompressFlow")
                dir.mkdirs()
                val destFile = File(dir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)
                return destFile.absolutePath
            }
        } catch (_: Exception) {
            return null
        }
    }
}
