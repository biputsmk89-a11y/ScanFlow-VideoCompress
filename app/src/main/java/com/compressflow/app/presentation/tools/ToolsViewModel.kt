package com.compressflow.app.presentation.tools

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import com.compressflow.app.data.repository.HistoryRepository
import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.OutputContainer
import com.compressflow.app.domain.model.VideoCodec
import com.compressflow.app.domain.model.VideoMetadata
import com.compressflow.app.media.analyzer.VideoAnalyzer
import com.compressflow.app.media.capability.CapabilityDetector
import com.compressflow.app.media.transformer.TransformerProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Tool types available in the Tools screen.
 */
enum class ToolType {
    REMOVE_AUDIO,
    EXTRACT_AUDIO,
    REMOVE_METADATA,
    VIDEO_INFO,
    RE_ENCODE
}

/**
 * Processing state for tool operations.
 */
sealed class ToolProcessingState {
    data object Idle : ToolProcessingState()
    data class Processing(
        val progress: Float = 0f,
        val toolName: String = ""
    ) : ToolProcessingState()

    data class Completed(
        val toolName: String,
        val outputPath: String,
        val outputSize: Long,
        val originalSize: Long
    ) : ToolProcessingState()

    data class VideoInfo(val metadata: VideoMetadata) : ToolProcessingState()
    data class Error(val message: String) : ToolProcessingState()
}

/**
 * ViewModel for tool operations: Remove Audio, Extract Audio,
 * Remove Metadata, Video Info, and Re-encode.
 *
 * Uses the existing Media3 Transformer engine and Android MediaExtractor/MediaMuxer
 * to perform real processing without any dummy/gimmick behavior.
 * Integrated with Room Database history and MediaStore.
 */
@OptIn(UnstableApi::class)
class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val videoAnalyzer = VideoAnalyzer(application)
    private val transformerProcessor = TransformerProcessor(application)
    private val historyRepository = HistoryRepository(application)
    val capabilities: CapabilityDetector.DeviceCapabilities = CapabilityDetector().detect()

    private val _state = MutableStateFlow<ToolProcessingState>(ToolProcessingState.Idle)
    val state: StateFlow<ToolProcessingState> = _state.asStateFlow()

    private var currentJob: Job? = null

    fun resetState() {
        currentJob?.cancel()
        _state.value = ToolProcessingState.Idle
    }

    fun processVideo(uri: Uri, tool: ToolType, targetCodec: VideoCodec? = null) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            when (tool) {
                ToolType.REMOVE_AUDIO -> removeAudio(uri)
                ToolType.EXTRACT_AUDIO -> extractAudio(uri)
                ToolType.REMOVE_METADATA -> removeMetadata(uri)
                ToolType.VIDEO_INFO -> showVideoInfo(uri)
                ToolType.RE_ENCODE -> reEncode(uri, targetCodec ?: VideoCodec.H264)
            }
        }
    }

    fun cancelProcessing() {
        currentJob?.cancel()
        transformerProcessor.cancel()
        _state.value = ToolProcessingState.Idle
    }

    // ── Remove Audio ─────────────────────────────────────────────
    // Uses Media3 Transformer to re-encode video without audio track.
    private suspend fun removeAudio(uri: Uri) {
        _state.value = ToolProcessingState.Processing(0f, "Remove Audio")

        val meta = analyzeOrFail(uri) ?: return

        if (!meta.hasAudio) {
            _state.value = ToolProcessingState.Error("This video already has no audio track")
            return
        }

        val app = getApplication<Application>()
        val plan = CompressionPlan(
            targetWidth = meta.width,
            targetHeight = meta.height,
            targetFps = meta.fps.takeIf { it > 0 } ?: 30f,
            targetVideoBitrate = meta.videoBitrate.takeIf { it > 0 } ?: 5_000_000L,
            targetAudioBitrate = 0L,
            videoCodec = detectOriginalCodec(meta),
            container = OutputContainer.MP4,
            removeAudio = true,
            estimatedOutputSize = 0L
        )

        val baseName = meta.filename?.substringBeforeLast(".")?.take(50) ?: "video"
        val outputDir = File(app.cacheDir, "tools").apply { mkdirs() }
        val outputFile = File(outputDir, "${baseName}_noaudio.mp4")

        runTransformer(uri, outputFile, plan, meta, "Remove Audio", "${baseName}_noaudio.mp4")
    }

    // ── Extract Audio ────────────────────────────────────────────
    // Uses Android MediaExtractor + MediaMuxer to extract audio track as M4A.
    // No re-encoding — pure track copy (near-instant speed).
    private suspend fun extractAudio(uri: Uri) {
        _state.value = ToolProcessingState.Processing(0f, "Extract Audio")

        val meta = analyzeOrFail(uri) ?: return

        if (!meta.hasAudio) {
            _state.value = ToolProcessingState.Error("This video has no audio track to extract")
            return
        }

        val app = getApplication<Application>()

        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(app, uri, null)

                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = format
                        break
                    }
                }

                if (audioTrackIndex < 0 || audioFormat == null) {
                    _state.value = ToolProcessingState.Error("No audio track found in this video")
                    return@withContext
                }

                extractor.selectTrack(audioTrackIndex)

                val baseName = meta.filename?.substringBeforeLast(".")?.take(50) ?: "audio"
                val outputDir = File(app.cacheDir, "tools").apply { mkdirs() }
                val outputFile = File(outputDir, "${baseName}_audio.m4a")

                val muxer = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                val muxerTrackIndex = muxer.addTrack(audioFormat)
                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()
                val durationUs = try {
                    audioFormat.getLong(MediaFormat.KEY_DURATION)
                } catch (_: Exception) {
                    meta.duration * 1000L
                }

                _state.value = ToolProcessingState.Processing(0.1f, "Extract Audio")

                var sampleCount = 0
                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()

                    sampleCount++
                    if (durationUs > 0 && sampleCount % 100 == 0) {
                        val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs)
                            .coerceIn(0.1f, 0.95f)
                        _state.value = ToolProcessingState.Processing(progress, "Extract Audio")
                    }
                }

                if (sampleCount > 0) {
                    try { muxer.stop() } catch (_: Exception) {}
                }
                try { muxer.release() } catch (_: Exception) {}

                val finalOutputSize = outputFile.length()
                val savedPath = copyToPublicStorage(
                    outputFile, "${baseName}_audio.m4a", isAudio = true
                )
                val finalPath = savedPath ?: outputFile.absolutePath

                recordHistory(
                    filename = "${baseName}_audio.m4a",
                    originalSize = meta.fileSize,
                    compressedSize = finalOutputSize,
                    durationMs = meta.duration,
                    resolution = "Audio Track",
                    codec = "M4A/AAC",
                    outputPath = finalPath
                )

                _state.value = ToolProcessingState.Completed(
                    toolName = "Extract Audio",
                    outputPath = finalPath,
                    outputSize = finalOutputSize,
                    originalSize = meta.fileSize
                )
            } catch (e: Exception) {
                _state.value = ToolProcessingState.Error(
                    "Extract audio failed: ${e.message}"
                )
            } finally {
                extractor.release()
            }
        }
    }

    // ── Remove Metadata ──────────────────────────────────────────
    // Uses MediaExtractor + MediaMuxer to remux video+audio tracks only,
    // stripping all metadata tracks (GPS, device info, timestamps, etc.).
    // No re-encoding — pure track copy (fast).
    private suspend fun removeMetadata(uri: Uri) {
        _state.value = ToolProcessingState.Processing(0f, "Remove Metadata")

        val meta = analyzeOrFail(uri) ?: return
        val app = getApplication<Application>()

        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(app, uri, null)

                val baseName = meta.filename?.substringBeforeLast(".")?.take(50) ?: "video"
                val outputDir = File(app.cacheDir, "tools").apply { mkdirs() }
                val outputFile = File(outputDir, "${baseName}_clean.mp4")

                val muxer = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )

                // Add only video and audio tracks (skip metadata/subtitle tracks)
                val trackMapping = mutableMapOf<Int, Int>()
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        val muxerTrack = muxer.addTrack(format)
                        trackMapping[i] = muxerTrack
                        extractor.selectTrack(i)
                    }
                }

                if (trackMapping.isEmpty()) {
                    _state.value = ToolProcessingState.Error(
                        "No valid video/audio tracks found"
                    )
                    return@withContext
                }

                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()
                val durationUs = meta.duration * 1000L

                _state.value = ToolProcessingState.Processing(0.1f, "Remove Metadata")

                var sampleCount = 0
                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val trackIndex = extractor.sampleTrackIndex
                    val muxerTrack = trackMapping[trackIndex]

                    if (muxerTrack != null) {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                    }

                    extractor.advance()
                    sampleCount++

                    if (durationUs > 0 && sampleCount % 200 == 0) {
                        val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs)
                            .coerceIn(0.1f, 0.95f)
                        _state.value = ToolProcessingState.Processing(
                            progress, "Remove Metadata"
                        )
                    }
                }

                if (sampleCount > 0) {
                    try { muxer.stop() } catch (_: Exception) {}
                }
                try { muxer.release() } catch (_: Exception) {}

                val finalOutputSize = outputFile.length()
                val savedPath = copyToPublicStorage(outputFile, "${baseName}_clean.mp4")
                val finalPath = savedPath ?: outputFile.absolutePath

                recordHistory(
                    filename = "${baseName}_clean.mp4",
                    originalSize = meta.fileSize,
                    compressedSize = finalOutputSize,
                    durationMs = meta.duration,
                    resolution = "${meta.width}×${meta.height}",
                    codec = meta.videoCodec ?: "H.264",
                    outputPath = finalPath
                )

                _state.value = ToolProcessingState.Completed(
                    toolName = "Remove Metadata",
                    outputPath = finalPath,
                    outputSize = finalOutputSize,
                    originalSize = meta.fileSize
                )
            } catch (e: Exception) {
                _state.value = ToolProcessingState.Error(
                    "Remove metadata failed: ${e.message}"
                )
            } finally {
                extractor.release()
            }
        }
    }

    // ── Video Info ────────────────────────────────────────────────
    // Uses VideoAnalyzer to show full technical metadata of any video.
    private suspend fun showVideoInfo(uri: Uri) {
        _state.value = ToolProcessingState.Processing(0.5f, "Analyzing Video")
        val meta = analyzeOrFail(uri) ?: return
        _state.value = ToolProcessingState.VideoInfo(meta)
    }

    // ── Re-encode (Video Converter) ──────────────────────────────
    // Uses Media3 Transformer to re-encode video with user-chosen codec
    // at the same resolution and quality.
    private suspend fun reEncode(uri: Uri, targetCodec: VideoCodec) {
        _state.value = ToolProcessingState.Processing(0f, "Re-encode")

        val meta = analyzeOrFail(uri) ?: return

        // Validate device codec support
        when (targetCodec) {
            VideoCodec.H265 -> if (!capabilities.supportsH265) {
                _state.value = ToolProcessingState.Error(
                    "Your device does not support H.265/HEVC hardware encoding"
                )
                return
            }

            VideoCodec.AV1 -> if (!capabilities.supportsAv1) {
                _state.value = ToolProcessingState.Error(
                    "Your device does not support AV1 hardware encoding"
                )
                return
            }

            else -> { /* H.264 always supported */ }
        }

        val app = getApplication<Application>()
        val plan = CompressionPlan(
            targetWidth = meta.width,
            targetHeight = meta.height,
            targetFps = meta.fps.takeIf { it > 0 } ?: 30f,
            targetVideoBitrate = meta.videoBitrate.takeIf { it > 0 } ?: 5_000_000L,
            targetAudioBitrate = meta.audioBitrate.takeIf { it > 0 } ?: 128_000L,
            videoCodec = targetCodec,
            container = OutputContainer.MP4,
            removeAudio = false,
            estimatedOutputSize = 0L
        )

        val baseName = meta.filename?.substringBeforeLast(".")?.take(50) ?: "video"
        val codecSuffix = targetCodec.displayName
            .replace("/", "").replace(".", "").lowercase()
        val outputDir = File(app.cacheDir, "tools").apply { mkdirs() }
        val outputFile = File(outputDir, "${baseName}_${codecSuffix}.mp4")
        val displayName = "${baseName}_${codecSuffix}.mp4"

        runTransformer(uri, outputFile, plan, meta, "Re-encode", displayName)
    }

    // ── Shared Helpers ───────────────────────────────────────────

    /**
     * Record completed video operation into Room Database history.
     */
    private suspend fun recordHistory(
        filename: String,
        originalSize: Long,
        compressedSize: Long,
        durationMs: Long,
        resolution: String,
        codec: String,
        outputPath: String
    ) {
        try {
            val savedPct = if (originalSize > 0) {
                ((originalSize - compressedSize).toFloat() / originalSize * 100f).coerceAtLeast(0f)
            } else 0f
            historyRepository.insertHistory(
                CompressionHistoryEntity(
                    filename = filename,
                    originalSize = originalSize,
                    compressedSize = compressedSize,
                    savedPercentage = savedPct,
                    durationMs = durationMs,
                    resolution = resolution,
                    codec = codec,
                    outputPath = outputPath
                )
            )
        } catch (_: Exception) {}
    }

    /**
     * Play processed output using system intent.
     */
    fun playOutput(outputPath: String, isAudio: Boolean = false) {
        val app = getApplication<Application>()
        try {
            val uri = if (outputPath.startsWith("content://")) {
                Uri.parse(outputPath)
            } else {
                val file = File(outputPath)
                if (!file.exists()) return
                FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            }
            val mimeType = if (isAudio) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Share processed output using system chooser.
     */
    fun shareOutput(outputPath: String, isAudio: Boolean = false) {
        val app = getApplication<Application>()
        try {
            val uri = if (outputPath.startsWith("content://")) {
                Uri.parse(outputPath)
            } else {
                val file = File(outputPath)
                if (!file.exists()) return
                FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            }
            val mimeType = if (isAudio) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
        } catch (_: Exception) {}
    }

    /**
     * Analyze video or set error state and return null.
     */
    private suspend fun analyzeOrFail(uri: Uri): VideoMetadata? {
        val result = videoAnalyzer.analyze(uri)
        if (result.isFailure) {
            _state.value = ToolProcessingState.Error(
                result.exceptionOrNull()?.message ?: "Failed to analyze video"
            )
            return null
        }
        return result.getOrNull()
    }

    /**
     * Detect the original video codec to preserve it during re-muxing operations.
     */
    private fun detectOriginalCodec(meta: VideoMetadata): VideoCodec {
        return when {
            meta.videoCodec?.contains("hevc", true) == true
                    && capabilities.supportsH265 -> VideoCodec.H265

            meta.videoCodec?.contains("av01", true) == true
                    && capabilities.supportsAv1 -> VideoCodec.AV1

            else -> VideoCodec.H264
        }
    }

    /**
     * Shared transformer execution for Remove Audio and Re-encode tools.
     */
    private suspend fun runTransformer(
        uri: Uri,
        outputFile: File,
        plan: CompressionPlan,
        meta: VideoMetadata,
        toolName: String,
        displayName: String
    ) {
        try {
            transformerProcessor.compress(
                inputUri = uri,
                outputFile = outputFile,
                plan = plan,
                originalSize = meta.fileSize
            ).collect { event ->
                when (event) {
                    is TransformerProcessor.ProcessEvent.Progress -> {
                        _state.value = ToolProcessingState.Processing(
                            event.fraction.coerceIn(0f, 0.99f), toolName
                        )
                    }

                    is TransformerProcessor.ProcessEvent.Complete -> {
                        val outputSize = event.result.outputSize
                        val savedPath = copyToPublicStorage(outputFile, displayName)
                        val finalPath = savedPath ?: outputFile.absolutePath

                        recordHistory(
                            filename = displayName,
                            originalSize = meta.fileSize,
                            compressedSize = outputSize,
                            durationMs = meta.duration,
                            resolution = "${plan.targetWidth}×${plan.targetHeight}",
                            codec = plan.videoCodec.displayName,
                            outputPath = finalPath
                        )

                        _state.value = ToolProcessingState.Completed(
                            toolName = toolName,
                            outputPath = finalPath,
                            outputSize = outputSize,
                            originalSize = meta.fileSize
                        )
                    }

                    is TransformerProcessor.ProcessEvent.Error -> {
                        _state.value = ToolProcessingState.Error(event.message)
                    }

                    is TransformerProcessor.ProcessEvent.FallbackApplied -> {
                        android.util.Log.i(
                            "ToolsViewModel",
                            "Codec fallback: ${event.originalMimeType} → ${event.fallbackMimeType}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (_state.value !is ToolProcessingState.Error) {
                _state.value = ToolProcessingState.Error(
                    e.message ?: "$toolName failed"
                )
            }
        }
    }

    /**
     * Copy processed file from cache to public storage (Movies/CompressFlow or Music/CompressFlow)
     * using MediaStore API (Android 10+) with legacy fallback.
     */
    @Suppress("DEPRECATION")
    private fun copyToPublicStorage(
        sourceFile: File,
        displayName: String,
        isAudio: Boolean = false
    ): String? {
        val app = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    if (isAudio) {
                        put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/CompressFlow")
                    } else {
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/CompressFlow")
                    }
                }

                val collection = if (isAudio) {
                    MediaStore.Audio.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                } else {
                    MediaStore.Video.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                }

                val uri = app.contentResolver.insert(collection, contentValues)
                    ?: return null
                app.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                sourceFile.delete()
                return uri.toString()
            } else {
                val dir = if (isAudio) {
                    File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_MUSIC
                        ), "CompressFlow"
                    )
                } else {
                    File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_MOVIES
                        ), "CompressFlow"
                    )
                }
                dir.mkdirs()
                val destFile = File(dir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)
                sourceFile.delete()
                return destFile.absolutePath
            }
        } catch (_: Exception) {
            return null
        }
    }
}
