package com.compressflow.app.media.analyzer

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.compressflow.app.domain.model.VideoMetadata
import com.compressflow.app.domain.model.VideoOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts comprehensive metadata from a video file.
 * Uses MediaMetadataRetriever + MediaExtractor for maximum data extraction.
 */
class VideoAnalyzer(private val context: Context) {

    suspend fun analyze(uri: Uri): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)

                val width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 0

                val height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 0

                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L

                val rotation = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                )?.toIntOrNull() ?: 0

                val bitrate = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_BITRATE
                )?.toLongOrNull() ?: 0L

                val mimeType = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                )

                // Get file info from content resolver
                val fileInfo = getFileInfo(uri)

                // Extract codec info from MediaExtractor
                val codecInfo = extractCodecInfo(uri)

                // Determine orientation
                val effectiveWidth = if (rotation == 90 || rotation == 270) height else width
                val effectiveHeight = if (rotation == 90 || rotation == 270) width else height
                val orientation = when {
                    effectiveWidth > effectiveHeight -> VideoOrientation.LANDSCAPE
                    effectiveHeight > effectiveWidth -> VideoOrientation.PORTRAIT
                    else -> VideoOrientation.SQUARE
                }

                val metadata = VideoMetadata(
                    uri = uri.toString(),
                    filename = fileInfo.first,
                    mimeType = mimeType,
                    fileSize = fileInfo.second,
                    duration = duration,
                    width = width,
                    height = height,
                    rotation = rotation,
                    fps = codecInfo.fps,
                    videoBitrate = bitrate,
                    videoCodec = codecInfo.videoCodec,
                    audioCodec = codecInfo.audioCodec,
                    audioBitrate = codecInfo.audioBitrate,
                    audioSampleRate = codecInfo.audioSampleRate,
                    audioChannels = codecInfo.audioChannels,
                    hasAudio = codecInfo.hasAudio,
                    isHdr = detectHdr(retriever),
                    orientation = orientation
                )

                Result.success(metadata)
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileInfo(uri: Uri): Pair<String?, Long> {
        var filename: String? = null
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) filename = cursor.getString(nameIndex)
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }

        return Pair(filename, size)
    }

    private data class CodecInfo(
        val videoCodec: String? = null,
        val audioCodec: String? = null,
        val fps: Float = 0f,
        val audioBitrate: Long = 0L,
        val audioSampleRate: Int = 0,
        val audioChannels: Int = 0,
        val hasAudio: Boolean = false
    )

    private fun extractCodecInfo(uri: Uri): CodecInfo {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)

            var videoCodec: String? = null
            var audioCodec: String? = null
            var fps = 0f
            var audioBitrate = 0L
            var audioSampleRate = 0
            var audioChannels = 0
            var hasAudio = false

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/")) {
                    videoCodec = mime
                    fps = try {
                        format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                    } catch (_: Exception) { 0f }
                } else if (mime.startsWith("audio/")) {
                    hasAudio = true
                    audioCodec = mime
                    audioBitrate = try {
                        format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                    } catch (_: Exception) { 0L }
                    audioSampleRate = try {
                        format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    } catch (_: Exception) { 0 }
                    audioChannels = try {
                        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    } catch (_: Exception) { 0 }
                }
            }

            return CodecInfo(videoCodec, audioCodec, fps, audioBitrate, audioSampleRate, audioChannels, hasAudio)
        } catch (_: Exception) {
            return CodecInfo()
        } finally {
            extractor.release()
        }
    }

    private fun detectHdr(retriever: MediaMetadataRetriever): Boolean {
        return try {
            val colorTransfer = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER
            )?.toIntOrNull()
            // HDR10 (ST2084) = 6, HLG = 7
            colorTransfer == 6 || colorTransfer == 7
        } catch (_: Exception) {
            false
        }
    }
}
