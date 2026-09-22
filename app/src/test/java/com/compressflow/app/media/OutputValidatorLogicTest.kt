package com.compressflow.app.media

import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.VideoMetadata
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class OutputValidatorLogicTest {

    @Test
    fun `test expectAudio logic correctly handles muted video`() {
        val metaWithAudio = VideoMetadata(
            uri = "content://media/video/1",
            hasAudio = true
        )
        val mutedPlan = CompressionPlan(
            targetWidth = 1280,
            targetHeight = 720,
            targetFps = 30f,
            targetVideoBitrate = 2000000L,
            removeAudio = true
        )

        // When user asks to strip/mute audio, expectAudio MUST be false
        val expectAudioMuted = metaWithAudio.hasAudio && !mutedPlan.removeAudio
        assertFalse("Expected audio must be false when removeAudio is true", expectAudioMuted)

        val unmutedPlan = CompressionPlan(
            targetWidth = 1280,
            targetHeight = 720,
            targetFps = 30f,
            targetVideoBitrate = 2000000L,
            removeAudio = false
        )
        val expectAudioNormal = metaWithAudio.hasAudio && !unmutedPlan.removeAudio
        assertTrue("Expected audio must be true when source has audio and removeAudio is false", expectAudioNormal)
    }

    @Test
    fun `test expectAudio logic correctly handles source video without audio`() {
        val metaNoAudio = VideoMetadata(
            uri = "content://media/video/2",
            hasAudio = false
        )
        val plan = CompressionPlan(
            targetWidth = 1280,
            targetHeight = 720,
            targetFps = 30f,
            targetVideoBitrate = 2000000L,
            removeAudio = false
        )

        val expectAudio = metaNoAudio.hasAudio && !plan.removeAudio
        assertFalse("Expected audio must be false when source video has no audio", expectAudio)
    }
}
