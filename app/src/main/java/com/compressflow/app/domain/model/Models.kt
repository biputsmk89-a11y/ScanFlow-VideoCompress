package com.compressflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents extracted metadata from a video file.
 * All fields are nullable because metadata extraction may fail partially.
 */
@Serializable
data class VideoMetadata(
    val uri: String,
    val filename: String? = null,
    val mimeType: String? = null,
    val fileSize: Long = 0L,
    val duration: Long = 0L, // milliseconds
    val width: Int = 0,
    val height: Int = 0,
    val rotation: Int = 0,
    val fps: Float = 0f,
    val videoBitrate: Long = 0L,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val audioBitrate: Long = 0L,
    val audioSampleRate: Int = 0,
    val audioChannels: Int = 0,
    val hasAudio: Boolean = true,
    val isHdr: Boolean = false,
    val orientation: VideoOrientation = VideoOrientation.LANDSCAPE
)

enum class VideoOrientation {
    LANDSCAPE,
    PORTRAIT,
    SQUARE
}

/**
 * Represents a compression plan — the output configuration.
 */
@Serializable
data class CompressionPlan(
    val targetWidth: Int,
    val targetHeight: Int,
    val targetFps: Float,
    val targetVideoBitrate: Long,
    val targetAudioBitrate: Long = 128_000L,
    val videoCodec: VideoCodec = VideoCodec.H264,
    val container: OutputContainer = OutputContainer.MP4,
    val removeAudio: Boolean = false,
    val estimatedOutputSize: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L
)

enum class VideoCodec(val displayName: String, val mimeType: String) {
    H264("H.264", "video/avc"),
    H265("H.265/HEVC", "video/hevc"),
    AV1("AV1", "video/av01")
}

enum class OutputContainer(val extension: String) {
    MP4("mp4"),
    MKV("mkv"),
    WEBM("webm")
}

/**
 * Smart preset definitions.
 */
@Serializable
enum class CompressionPreset(
    val displayName: String,
    val description: String,
    val maxFileSize: Long? = null,        // bytes, null = no target
    val maxWidth: Int = 1920,
    val maxHeight: Int = 1080,
    val maxFps: Float = 30f,
    val preferredCodec: VideoCodec = VideoCodec.H264,
    val qualityFactor: Float = 0.7f       // 0.0 = max compression, 1.0 = max quality
) {
    WHATSAPP(
        displayName = "WhatsApp",
        description = "< 16 MB or 64 MB",
        maxFileSize = 64 * 1024 * 1024L,
        maxWidth = 1280,
        maxHeight = 720,
        maxFps = 30f,
        qualityFactor = 0.5f
    ),
    SOCIAL_MEDIA(
        displayName = "Social Media",
        description = "1080p • 30fps Crisp",
        maxWidth = 1920,
        maxHeight = 1080,
        maxFps = 30f,
        qualityFactor = 0.7f
    ),
    STORAGE_SAVER(
        displayName = "Storage Saver",
        description = "~70% space saved",
        maxWidth = 1280,
        maxHeight = 720,
        maxFps = 30f,
        qualityFactor = 0.4f
    ),
    EMAIL(
        displayName = "Email Friendly",
        description = "< 25 MB • Fast",
        maxFileSize = 25 * 1024 * 1024L,
        maxWidth = 854,
        maxHeight = 480,
        maxFps = 24f,
        qualityFactor = 0.3f
    ),
    TELEGRAM(
        displayName = "Telegram",
        description = "Up to 2 GB",
        maxFileSize = 2L * 1024 * 1024 * 1024,
        maxWidth = 1920,
        maxHeight = 1080,
        maxFps = 30f,
        qualityFactor = 0.65f
    ),
    UPLOAD(
        displayName = "Upload",
        description = "Balanced quality & size",
        maxWidth = 1920,
        maxHeight = 1080,
        maxFps = 30f,
        qualityFactor = 0.6f
    ),
    TARGET_SIZE(
        displayName = "Target Size",
        description = "Choose output file size",
        qualityFactor = 0.5f
    ),
    CUSTOM(
        displayName = "Custom",
        description = "Full control",
        qualityFactor = 0.7f
    )
}

/**
 * Quality tiers for simple mode.
 */
enum class QualityTier(val displayName: String, val factor: Float) {
    MAXIMUM("Maximum Quality", 0.95f),
    HIGH("High", 0.8f),
    BALANCED("Balanced", 0.6f),
    SMALL("Small", 0.4f),
    MAXIMUM_COMPRESSION("Maximum Compression", 0.2f)
}

/**
 * Compression result after processing.
 */
data class CompressionResult(
    val success: Boolean,
    val outputUri: String? = null,
    val outputSize: Long = 0L,
    val originalSize: Long = 0L,
    val compressionRatio: Float = 0f,
    val savedBytes: Long = 0L,
    val savedPercentage: Float = 0f,
    val duration: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Compression state for UI observation.
 */
sealed class CompressionState {
    data object Idle : CompressionState()
    data class Analyzing(val progress: Float = 0f) : CompressionState()
    data class Planning(val plan: CompressionPlan? = null) : CompressionState()
    data class Compressing(
        val progress: Float = 0f,
        val currentSize: Long = 0L,
        val estimatedSize: Long = 0L
    ) : CompressionState()
    data class Paused(val progress: Float = 0f) : CompressionState()
    data class Completed(val result: CompressionResult) : CompressionState()
    data class Failed(val error: String) : CompressionState()
    data object Cancelled : CompressionState()
}
