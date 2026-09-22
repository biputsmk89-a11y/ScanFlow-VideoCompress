package com.compressflow.app.media.capability

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import com.compressflow.app.domain.model.VideoCodec

/**
 * Detects device hardware encoder capabilities at runtime.
 * Never assumes encoder support — always probes the device.
 */
class CapabilityDetector {

    data class DeviceCapabilities(
        val androidVersion: Int = Build.VERSION.SDK_INT,
        val supportsH264: Boolean = false,
        val supportsH265: Boolean = false,
        val supportsAv1: Boolean = false,
        val h264HardwareEncoder: Boolean = false,
        val h265HardwareEncoder: Boolean = false,
        val av1HardwareEncoder: Boolean = false,
        val maxSupportedWidth: Int = 1920,
        val maxSupportedHeight: Int = 1080,
        val maxSupportedFrameRate: Int = 30,
        val supportedCodecs: List<VideoCodec> = emptyList()
    )

    fun detect(): DeviceCapabilities {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos

        var supportsH264 = false
        var supportsH265 = false
        var supportsAv1 = false
        var h264Hw = false
        var h265Hw = false
        var av1Hw = false
        var maxWidth = 1920
        var maxHeight = 1080
        var maxFps = 30

        for (info in codecInfos) {
            if (!info.isEncoder) continue

            for (type in info.supportedTypes) {
                when {
                    type.equals("video/avc", ignoreCase = true) -> {
                        supportsH264 = true
                        if (info.isHwEncoder()) h264Hw = true
                        updateMaxCaps(info, type)?.let { (w, h, f) ->
                            maxWidth = maxOf(maxWidth, w)
                            maxHeight = maxOf(maxHeight, h)
                            maxFps = maxOf(maxFps, f)
                        }
                    }
                    type.equals("video/hevc", ignoreCase = true) -> {
                        supportsH265 = true
                        if (info.isHwEncoder()) h265Hw = true
                        updateMaxCaps(info, type)?.let { (w, h, f) ->
                            maxWidth = maxOf(maxWidth, w)
                            maxHeight = maxOf(maxHeight, h)
                            maxFps = maxOf(maxFps, f)
                        }
                    }
                    type.equals("video/av01", ignoreCase = true) -> {
                        supportsAv1 = true
                        if (info.isHwEncoder()) av1Hw = true
                    }
                }
            }
        }

        val supportedCodecs = mutableListOf<VideoCodec>()
        if (supportsH264) supportedCodecs.add(VideoCodec.H264)
        if (supportsH265) supportedCodecs.add(VideoCodec.H265)
        if (supportsAv1) supportedCodecs.add(VideoCodec.AV1)

        return DeviceCapabilities(
            supportsH264 = supportsH264,
            supportsH265 = supportsH265,
            supportsAv1 = supportsAv1,
            h264HardwareEncoder = h264Hw,
            h265HardwareEncoder = h265Hw,
            av1HardwareEncoder = av1Hw,
            maxSupportedWidth = maxWidth,
            maxSupportedHeight = maxHeight,
            maxSupportedFrameRate = maxFps,
            supportedCodecs = supportedCodecs
        )
    }

    private fun updateMaxCaps(
        info: MediaCodecInfo,
        mimeType: String
    ): Triple<Int, Int, Int>? {
        return try {
            val caps = info.getCapabilitiesForType(mimeType)
            val videoCaps = caps.videoCapabilities ?: return null
            Triple(
                videoCaps.supportedWidths.upper,
                videoCaps.supportedHeights.upper,
                videoCaps.supportedFrameRates.upper.toInt()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun MediaCodecInfo.isHwEncoder(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isHardwareAccelerated
        } else {
            // Heuristic: hardware encoders typically don't have "OMX.google" prefix
            !name.startsWith("OMX.google.", ignoreCase = true) &&
                !name.startsWith("c2.android.", ignoreCase = true)
        }
    }

    /**
     * Choose the best codec for the device.
     * Prefers hardware-accelerated HEVC > H264.
     */
    fun bestCodec(capabilities: DeviceCapabilities): VideoCodec {
        return when {
            capabilities.h265HardwareEncoder -> VideoCodec.H265
            capabilities.supportsH265 -> VideoCodec.H265
            capabilities.h264HardwareEncoder -> VideoCodec.H264
            else -> VideoCodec.H264
        }
    }
}
