package com.compressflow.app.media.planner

import com.compressflow.app.domain.model.*
import com.compressflow.app.media.capability.CapabilityDetector
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Core compression planner.
 * Input: VideoMetadata + UserGoal + DeviceCapabilities
 * Output: CompressionPlan
 *
 * Deterministic and testable — no Android framework dependencies.
 */
class CompressionPlanner {

    /**
     * Create a compression plan from a preset.
     */
    fun plan(
        metadata: VideoMetadata,
        preset: CompressionPreset,
        capabilities: CapabilityDetector.DeviceCapabilities,
        targetSizeBytes: Long? = null,
        qualityFactorOverride: Float? = null
    ): CompressionPlan {
        // 1. Determine target resolution
        val (targetWidth, targetHeight) = calculateTargetResolution(
            srcWidth = metadata.width,
            srcHeight = metadata.height,
            rotation = metadata.rotation,
            maxWidth = preset.maxWidth,
            maxHeight = preset.maxHeight,
            maxDeviceWidth = capabilities.maxSupportedWidth,
            maxDeviceHeight = capabilities.maxSupportedHeight
        )

        // 2. Determine target FPS
        val targetFps = min(metadata.fps, preset.maxFps).let { fps ->
            min(fps, capabilities.maxSupportedFrameRate.toFloat())
        }.coerceAtLeast(24f)

        // 3. Choose codec
        val codec = selectCodec(preset.preferredCodec, capabilities)

        // 4. Calculate bitrate
        val targetVideoBitrate = if (targetSizeBytes != null && targetSizeBytes > 0) {
            calculateBitrateForTargetSize(
                targetSizeBytes = targetSizeBytes,
                durationMs = metadata.duration,
                audioBitrate = 128_000L
            )
        } else {
            val effectiveQualityFactor = qualityFactorOverride ?: preset.qualityFactor
            calculateSmartBitrate(
                width = targetWidth,
                height = targetHeight,
                fps = targetFps,
                qualityFactor = effectiveQualityFactor,
                codec = codec
            )
        }

        // 5. Audio bitrate
        val audioTargetBitrate = if (preset == CompressionPreset.EMAIL) 64_000L else 128_000L

        // 6. Estimate output size
        val estimatedSize = estimateOutputSize(
            videoBitrate = targetVideoBitrate,
            audioBitrate = audioTargetBitrate,
            durationMs = metadata.duration
        )

        return CompressionPlan(
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            targetFps = targetFps,
            targetVideoBitrate = targetVideoBitrate,
            targetAudioBitrate = audioTargetBitrate,
            videoCodec = codec,
            container = OutputContainer.MP4,
            removeAudio = false,
            estimatedOutputSize = estimatedSize
        )
    }

    /**
     * Plan from a quality tier.
     */
    fun planFromQuality(
        metadata: VideoMetadata,
        quality: QualityTier,
        capabilities: CapabilityDetector.DeviceCapabilities
    ): CompressionPlan {
        val preset = when (quality) {
            QualityTier.MAXIMUM -> CompressionPreset.SOCIAL_MEDIA
            QualityTier.HIGH -> CompressionPreset.UPLOAD
            QualityTier.BALANCED -> CompressionPreset.SOCIAL_MEDIA
            QualityTier.SMALL -> CompressionPreset.STORAGE_SAVER
            QualityTier.MAXIMUM_COMPRESSION -> CompressionPreset.EMAIL
        }
        return plan(
            metadata = metadata,
            preset = preset,
            capabilities = capabilities,
            qualityFactorOverride = quality.factor
        )
    }

    // ── Internal helpers ────────────────────────────────────

    private fun calculateTargetResolution(
        srcWidth: Int, srcHeight: Int, rotation: Int,
        maxWidth: Int, maxHeight: Int,
        maxDeviceWidth: Int, maxDeviceHeight: Int
    ): Pair<Int, Int> {
        // Account for rotation
        val effectiveWidth = if (rotation == 90 || rotation == 270) srcHeight else srcWidth
        val effectiveHeight = if (rotation == 90 || rotation == 270) srcWidth else srcHeight

        val constrainedMaxW = min(maxWidth, maxDeviceWidth)
        val constrainedMaxH = min(maxHeight, maxDeviceHeight)

        if (effectiveWidth <= constrainedMaxW && effectiveHeight <= constrainedMaxH) {
            // Already within limits — keep original but make even
            return Pair(makeEven(effectiveWidth), makeEven(effectiveHeight))
        }

        // Scale down maintaining aspect ratio
        val scaleW = constrainedMaxW.toFloat() / effectiveWidth
        val scaleH = constrainedMaxH.toFloat() / effectiveHeight
        val scale = min(scaleW, scaleH)

        return Pair(
            makeEven((effectiveWidth * scale).roundToInt()),
            makeEven((effectiveHeight * scale).roundToInt())
        )
    }

    private fun makeEven(value: Int): Int = if (value % 2 == 0) value else value - 1

    private fun selectCodec(
        preferred: VideoCodec,
        capabilities: CapabilityDetector.DeviceCapabilities
    ): VideoCodec {
        return when (preferred) {
            VideoCodec.H265 -> if (capabilities.supportsH265) VideoCodec.H265 else VideoCodec.H264
            VideoCodec.AV1 -> when {
                capabilities.supportsAv1 -> VideoCodec.AV1
                capabilities.supportsH265 -> VideoCodec.H265
                else -> VideoCodec.H264
            }
            VideoCodec.H264 -> VideoCodec.H264
        }
    }

    /**
     * Calculate bitrate for a target file size.
     * targetBitrate ≈ (targetSize * 8 / duration) - audioBitrate
     * Apply 0.9 safety margin for container overhead.
     */
    private fun calculateBitrateForTargetSize(
        targetSizeBytes: Long,
        durationMs: Long,
        audioBitrate: Long
    ): Long {
        if (durationMs <= 0) return 2_000_000L

        val durationSec = durationMs / 1000.0
        val totalBitrate = ((targetSizeBytes * 8) / durationSec * 0.9).roundToLong()
        val videoBitrate = totalBitrate - audioBitrate

        return videoBitrate.coerceAtLeast(200_000L) // Minimum 200kbps
    }

    /**
     * Smart bitrate calculation based on resolution, fps, quality factor.
     * Base bitrate is per-pixel, scaled by quality factor and codec efficiency.
     */
    private fun calculateSmartBitrate(
        width: Int, height: Int, fps: Float,
        qualityFactor: Float, codec: VideoCodec
    ): Long {
        val pixels = width.toLong() * height
        // Base bits per pixel per frame
        val bpp = 0.1f * qualityFactor

        val baseBitrate = (pixels * bpp * fps).roundToLong()

        // HEVC is ~40% more efficient at same quality
        val codecFactor = when (codec) {
            VideoCodec.H265 -> 0.6f
            VideoCodec.AV1 -> 0.5f
            VideoCodec.H264 -> 1.0f
        }

        val bitrate = (baseBitrate * codecFactor).roundToLong()

        // Clamp to reasonable range
        return bitrate.coerceIn(200_000L, 50_000_000L)
    }

    /**
     * Estimate output file size.
     */
    fun estimateOutputSize(
        videoBitrate: Long,
        audioBitrate: Long,
        durationMs: Long
    ): Long {
        if (durationMs <= 0) return 0L
        val durationSec = durationMs / 1000.0
        val totalBits = (videoBitrate + audioBitrate) * durationSec
        // Add ~5% for container overhead
        return (totalBits / 8 * 1.05).roundToLong()
    }
}
