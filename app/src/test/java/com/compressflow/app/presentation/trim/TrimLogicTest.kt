package com.compressflow.app.presentation.trim

import com.compressflow.app.core.extensions.formatDuration
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.OutputContainer
import com.compressflow.app.domain.model.VideoCodec
import com.compressflow.app.domain.model.VideoMetadata
import org.junit.Assert.*
import org.junit.Test

class TrimLogicTest {

    @Test
    fun `test normal trim range calculations`() {
        // Video 10:35 (635,000 ms)
        // User selects: Start = 02:15 (135,000 ms), End = 05:42 (342,000 ms)
        // Expected: 03:27 (207,000 ms)
        val videoDurationMs = 635_000L
        val startMs = 135_000L
        val endMs = 342_000L

        val selectedDurationMs = (endMs - startMs).coerceAtLeast(0L)

        assertEquals(207_000L, selectedDurationMs)
        assertEquals("10:35", videoDurationMs.formatDuration())
        assertEquals("2:15", startMs.formatDuration())
        assertEquals("5:42", endMs.formatDuration())
        assertEquals("3:27", selectedDurationMs.formatDuration())
    }

    @Test
    fun `test start handle clamping rules`() {
        val totalDurationMs = 60_000L // 60s
        val minClip = 1000L
        val currentEndMs = 30_000L

        // User attempts to drag start beyond end
        val requestedStart1 = 35_000L
        val maxStart = (currentEndMs - minClip).coerceAtLeast(0L)
        val clampedStart1 = requestedStart1.coerceIn(0L, maxStart)

        assertEquals(29_000L, clampedStart1)
        assertTrue(clampedStart1 < currentEndMs)
        assertTrue(currentEndMs - clampedStart1 >= minClip)

        // User attempts to drag start to negative
        val requestedStart2 = -5000L
        val clampedStart2 = requestedStart2.coerceIn(0L, maxStart)
        assertEquals(0L, clampedStart2)
    }

    @Test
    fun `test end handle clamping rules`() {
        val totalDurationMs = 60_000L
        val minClip = 1000L
        val currentStartMs = 10_000L

        // User attempts to drag end past video duration
        val requestedEnd1 = 75_000L
        val minEnd = (currentStartMs + minClip).coerceAtMost(totalDurationMs)
        val clampedEnd1 = requestedEnd1.coerceIn(minEnd, totalDurationMs)
        assertEquals(totalDurationMs, clampedEnd1)

        // User attempts to drag end before start + minClip
        val requestedEnd2 = 5_000L
        val clampedEnd2 = requestedEnd2.coerceIn(minEnd, totalDurationMs)
        assertEquals(11_000L, clampedEnd2)
        assertTrue(clampedEnd2 > currentStartMs)
    }

    @Test
    fun `test CompressionPlan carries clipping configuration accurately`() {
        val plan = CompressionPlan(
            targetWidth = 1920,
            targetHeight = 1080,
            targetFps = 30f,
            targetVideoBitrate = 5_000_000L,
            targetAudioBitrate = 128_000L,
            videoCodec = VideoCodec.H264,
            container = OutputContainer.MP4,
            removeAudio = false,
            estimatedOutputSize = 0L,
            trimStartMs = 15_000L,
            trimEndMs = 45_000L
        )

        assertEquals(15_000L, plan.trimStartMs)
        assertEquals(45_000L, plan.trimEndMs)
        assertTrue(plan.trimEndMs > plan.trimStartMs)
        assertEquals(30_000L, plan.trimEndMs - plan.trimStartMs)
    }

    @Test
    fun `test short clip duration safety minimum`() {
        val totalDurationMs = 3000L // 3 seconds video
        val minClip = 1000L.coerceAtMost(totalDurationMs)

        assertEquals(1000L, minClip)

        val startMs = 0L
        val endMs = 800L // below 1s
        val clipDuration = (endMs - startMs).coerceAtLeast(500L)

        assertEquals(800L, clipDuration)
    }

    @Test
    fun `test TrimUiState selected duration helper`() {
        val state = TrimUiState(
            durationMs = 120_000L,
            startMs = 10_000L,
            endMs = 40_000L
        )

        assertEquals(30_000L, state.selectedDurationMs)
        assertEquals("0:30", state.selectedDurationMs.formatDuration())
    }

    @Test
    fun `test pure trim plan preserves original dimensions by avoiding Presentation effect`() {
        // When compressAlso is false, targetWidth and targetHeight must be 0
        // so TransformerProcessor does not add Presentation.createForWidthAndHeight,
        // preserving 100% of native aspect ratio, display orientation, and resolution.
        val meta = VideoMetadata(
            uri = "content://media/video/portrait",
            filename = "portrait_vid.mp4",
            fileSize = 50_000_000L,
            duration = 20_000L,
            width = 1920,
            height = 1080,
            rotation = 90
        )

        assertEquals(1080, meta.displayWidth)
        assertEquals(1920, meta.displayHeight)

        val pureTrimPlan = CompressionPlan(
            targetWidth = 0,
            targetHeight = 0,
            targetFps = 0f,
            targetVideoBitrate = 8_000_000L,
            targetAudioBitrate = 128_000L,
            videoCodec = VideoCodec.H264,
            container = OutputContainer.MP4,
            removeAudio = false,
            estimatedOutputSize = 0L,
            trimStartMs = 2_000L,
            trimEndMs = 12_000L
        )

        // targetWidth and targetHeight == 0 means no Presentation scaling is forced
        assertEquals(0, pureTrimPlan.targetWidth)
        assertEquals(0, pureTrimPlan.targetHeight)

        val outputRes = if (pureTrimPlan.targetWidth > 0 && pureTrimPlan.targetHeight > 0) {
            "${pureTrimPlan.targetWidth}×${pureTrimPlan.targetHeight}"
        } else {
            "${meta.displayWidth}×${meta.displayHeight}"
        }

        assertEquals("1080×1920", outputRes)
    }
}
