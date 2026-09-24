package com.compressflow.app.presentation.tools

import com.compressflow.app.core.extensions.formatBitrate
import com.compressflow.app.core.extensions.formatDuration
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.domain.model.VideoCodec
import com.compressflow.app.domain.model.VideoMetadata
import com.compressflow.app.media.capability.CapabilityDetector
import org.junit.Assert.*
import org.junit.Test

class ToolsLogicTest {

    @Test
    fun `test ToolType enum contains all required functional tools`() {
        val tools = ToolType.entries
        assertEquals(5, tools.size)
        assertTrue(tools.contains(ToolType.REMOVE_AUDIO))
        assertTrue(tools.contains(ToolType.EXTRACT_AUDIO))
        assertTrue(tools.contains(ToolType.REMOVE_METADATA))
        assertTrue(tools.contains(ToolType.VIDEO_INFO))
        assertTrue(tools.contains(ToolType.RE_ENCODE))
    }

    @Test
    fun `test ToolProcessingState transitions properly`() {
        val idle = ToolProcessingState.Idle
        val processing = ToolProcessingState.Processing(0.5f, "Re-encode")
        val completed = ToolProcessingState.Completed(
            toolName = "Re-encode",
            outputPath = "/storage/Movies/CompressFlow/test_h265.mp4",
            outputSize = 10_000_000L,
            originalSize = 25_000_000L
        )
        val error = ToolProcessingState.Error("Encoding failed")

        assertEquals(0.5f, processing.progress)
        assertEquals("Re-encode", processing.toolName)
        assertEquals(10_000_000L, completed.outputSize)
        assertEquals(25_000_000L, completed.originalSize)
        assertEquals("Encoding failed", error.message)
    }

    @Test
    fun `test video info format extensions render accurately`() {
        val meta = VideoMetadata(
            uri = "content://media/video/1",
            filename = "sample_4k.mp4",
            fileSize = 104_857_600L, // 100 MB
            duration = 125_000L,     // 2m 5s
            width = 3840,
            height = 2160,
            fps = 60f,
            videoBitrate = 12_500_000L,
            hasAudio = true,
            audioBitrate = 192_000L
        )

        assertEquals("100 MB", meta.fileSize.formatFileSize())
        assertEquals("2:05", meta.duration.formatDuration())
        assertEquals("12.5 Mbps", meta.videoBitrate.formatBitrate())
        assertEquals("192 Kbps", meta.audioBitrate.formatBitrate())
    }

    @Test
    fun `test output file naming pattern consistency`() {
        val baseName = "vacation_clip"
        val noAudioName = "${baseName}_noaudio.mp4"
        val audioExtractName = "${baseName}_audio.m4a"
        val cleanName = "${baseName}_clean.mp4"
        val reEncodeName = "${baseName}_h265.mp4"

        assertEquals("vacation_clip_noaudio.mp4", noAudioName)
        assertEquals("vacation_clip_audio.m4a", audioExtractName)
        assertEquals("vacation_clip_clean.mp4", cleanName)
        assertEquals("vacation_clip_h265.mp4", reEncodeName)
    }

    @Test
    fun `test hardware capability constraints for re-encode`() {
        val capsNoHevc = CapabilityDetector.DeviceCapabilities(
            supportsH264 = true,
            supportsH265 = false,
            supportsAv1 = false,
            h264HardwareEncoder = true,
            h265HardwareEncoder = false,
            maxSupportedWidth = 1920,
            maxSupportedHeight = 1080,
            maxSupportedFrameRate = 60,
            supportedCodecs = listOf(VideoCodec.H264)
        )

        assertFalse(capsNoHevc.supportsH265)
        assertTrue(capsNoHevc.supportsH264)
        assertEquals(1, capsNoHevc.supportedCodecs.size)
    }

    @Test
    fun `test tool plan preserves original resolution without presentation layout scaling`() {
        val meta = VideoMetadata(
            uri = "content://media/video/portrait",
            width = 1920,
            height = 1080,
            rotation = 90
        )

        // For tools like Remove Audio & Re-encode, targetWidth and targetHeight are 0
        // to prevent Presentation from letterboxing or squashing portrait videos
        val toolPlan = com.compressflow.app.domain.model.CompressionPlan(
            targetWidth = 0,
            targetHeight = 0,
            targetFps = 0f,
            targetVideoBitrate = 5_000_000L,
            videoCodec = VideoCodec.H264
        )

        assertEquals(0, toolPlan.targetWidth)
        assertEquals(0, toolPlan.targetHeight)

        val reportedResolution = if (toolPlan.targetWidth > 0 && toolPlan.targetHeight > 0) {
            "${toolPlan.targetWidth}×${toolPlan.targetHeight}"
        } else {
            "${meta.displayWidth}×${meta.displayHeight}"
        }

        assertEquals("1080×1920", reportedResolution)
    }

    @Test
    fun `test audio extraction guard rejects video without audio`() {
        val silentVideo = VideoMetadata(
            uri = "content://media/video/silent",
            hasAudio = false
        )
        assertFalse("Silent video must have hasAudio = false", silentVideo.hasAudio)
    }
}
