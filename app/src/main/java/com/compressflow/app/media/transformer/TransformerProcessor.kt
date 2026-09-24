package com.compressflow.app.media.transformer

import android.content.Context
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.CompressionResult
import com.compressflow.app.domain.model.OutputContainer
import com.compressflow.app.domain.model.VideoCodec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Enterprise-grade Media3 Transformer wrapper for video compression.
 * Configures:
 * 1. Spatial resolution scaling via Presentation effect
 * 2. Frame-rate limiter (FPS drop) via FrameDropEffect
 * 3. Requested bitrate allocation via VideoEncoderSettings (VBR) & AudioEncoderSettings
 * 4. Hardware & software encoder fallback (H.265 -> H.264) for emulators and physical devices
 * 5. HDR-to-SDR tone mapping for universal compatibility
 * 6. Flow-based real-time progress, telemetry alerts & graceful cancellation
 */
@UnstableApi
class TransformerProcessor(private val context: Context) {

    private var currentTransformer: Transformer? = null

    sealed class ProcessEvent {
        data class Progress(val fraction: Float) : ProcessEvent()
        data class FallbackApplied(
            val originalMimeType: String?,
            val fallbackMimeType: String?
        ) : ProcessEvent()
        data class Complete(val result: CompressionResult) : ProcessEvent()
        data class Error(val message: String) : ProcessEvent()
    }

    fun compress(
        inputUri: Uri,
        outputFile: File,
        plan: CompressionPlan,
        originalSize: Long
    ): Flow<ProcessEvent> = callbackFlow {
        val videoMimeType = when (plan.videoCodec) {
            VideoCodec.H264 -> MimeTypes.VIDEO_H264
            VideoCodec.H265 -> MimeTypes.VIDEO_H265
            VideoCodec.AV1 -> MimeTypes.VIDEO_AV1
        }

        // 1. Video Encoder Settings (Explicit Bitrate + VBR mode)
        val videoEncoderSettings = VideoEncoderSettings.Builder()
            .setBitrate(plan.targetVideoBitrate.toInt().coerceAtLeast(150_000))
            .setBitrateMode(android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            .build()

        // 2. Audio Encoder Settings (Explicit AAC Bitrate)
        val audioEncoderSettings = AudioEncoderSettings.Builder()
            .setBitrate(plan.targetAudioBitrate.toInt().coerceIn(32_000, 320_000))
            .build()

        // 3. Encoder Factory with Auto-Fallback
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(videoEncoderSettings)
            .setRequestedAudioEncoderSettings(audioEncoderSettings)
            .setEnableFallback(true) // Allows fallback to software encoder or H.264 if hardware/codec fails
            .build()

        // 4. Transformer Builder with Fallback Telemetry Listener
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(videoMimeType)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    val outputSize = outputFile.length()
                    val saved = originalSize - outputSize
                    val savedPct = if (originalSize > 0) {
                        (saved.toFloat() / originalSize * 100)
                    } else 0f

                    trySend(ProcessEvent.Complete(
                        CompressionResult(
                            success = true,
                            outputUri = Uri.fromFile(outputFile).toString(),
                            outputSize = outputSize,
                            originalSize = originalSize,
                            compressionRatio = if (originalSize > 0) outputSize.toFloat() / originalSize else 0f,
                            savedBytes = saved,
                            savedPercentage = savedPct,
                            duration = exportResult.durationMs
                        )
                    ))
                    close()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    trySend(ProcessEvent.Error(
                        exportException.message ?: "Compression failed: ${exportException.errorCodeName}"
                    ))
                    close()
                }

                override fun onFallbackApplied(
                    composition: Composition,
                    originalTransformationRequest: TransformationRequest,
                    fallbackTransformationRequest: TransformationRequest
                ) {
                    trySend(ProcessEvent.FallbackApplied(
                        originalMimeType = originalTransformationRequest.videoMimeType,
                        fallbackMimeType = fallbackTransformationRequest.videoMimeType
                    ))
                }
            })
            .build()

        currentTransformer = transformer

        // 5. Visual Effects Pipeline: Resolution Downscaling + Frame Rate Limiter + Precise Clipping
        val mediaItem = if (plan.trimEndMs > plan.trimStartMs) {
            MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(plan.trimStartMs.coerceAtLeast(0L))
                        .setEndPositionMs(plan.trimEndMs)
                        .build()
                )
                .build()
        } else {
            MediaItem.fromUri(inputUri)
        }
        val effectsList = mutableListOf<Effect>()

        if (plan.targetWidth > 0 && plan.targetHeight > 0) {
            effectsList.add(
                Presentation.createForWidthAndHeight(
                    plan.targetWidth,
                    plan.targetHeight,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            )
        }

        if (plan.targetFps > 0f) {
            effectsList.add(FrameDropEffect.createDefaultFrameDropEffect(plan.targetFps))
        }

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effectsList))
            .setRemoveAudio(plan.removeAudio)
            .build()

        // 6. Composition with Tone Mapping (HDR to SDR for universal playback)
        val composition = Composition.Builder(EditedMediaItemSequence.Builder(editedMediaItem).build())
            .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
            .build()

        // Start export
        transformer.start(composition, outputFile.absolutePath)

        // Poll progress
        val progressHolder = ProgressHolder()
        val progressJob = launch {
            while (true) {
                delay(200)
                val progressState = transformer.getProgress(progressHolder)
                if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                    trySend(ProcessEvent.Progress(progressHolder.progress / 100f))
                }
            }
        }

        awaitClose {
            progressJob.cancel()
            transformer.cancel()
            currentTransformer = null
        }
    }

    /**
     * Cancel current compression.
     */
    fun cancel() {
        currentTransformer?.cancel()
        currentTransformer = null
    }

    /**
     * Generate output file with predictable naming and correct container extension.
     */
    fun createOutputFile(
        context: Context,
        originalFilename: String?,
        container: OutputContainer = OutputContainer.MP4,
        pattern: String = "{name}_compressed"
    ): File {
        val timestamp = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            java.util.Locale.US
        ).format(java.util.Date())

        val baseName = originalFilename
            ?.substringBeforeLast(".")
            ?.take(50)
            ?: "video"

        val outputDir = File(context.cacheDir, "compressed").apply { mkdirs() }
        val filename = when (pattern) {
            "VID_{date}_{name}" -> "VID_${timestamp}_${baseName}.${container.extension}"
            "{name}_small" -> "${baseName}_small.${container.extension}"
            "{name}_cf" -> "${baseName}_cf.${container.extension}"
            else -> "${baseName}_compressed.${container.extension}"
        }

        return File(outputDir, filename)
    }

    /**
     * Clean up orphaned temporary files in cache older than [maxAgeHours].
     */
    fun cleanOrphanedCacheFiles(context: Context, maxAgeHours: Int = 24) {
        try {
            val outputDir = File(context.cacheDir, "compressed")
            if (!outputDir.exists() || !outputDir.isDirectory) return
            val cutoff = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000L)
            outputDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
