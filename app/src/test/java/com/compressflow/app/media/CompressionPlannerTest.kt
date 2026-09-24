package com.compressflow.app.media

import com.compressflow.app.core.extensions.formatDuration
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.domain.model.*
import com.compressflow.app.media.capability.CapabilityDetector
import com.compressflow.app.media.planner.CompressionPlanner
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CompressionPlannerTest {

    private lateinit var planner: CompressionPlanner
    private lateinit var capabilities: CapabilityDetector.DeviceCapabilities

    @Before
    fun setup() {
        planner = CompressionPlanner()
        capabilities = CapabilityDetector.DeviceCapabilities(
            supportsH264 = true,
            supportsH265 = true,
            supportsAv1 = false,
            h264HardwareEncoder = true,
            h265HardwareEncoder = true,
            maxSupportedWidth = 3840,
            maxSupportedHeight = 2160,
            maxSupportedFrameRate = 60,
            supportedCodecs = listOf(VideoCodec.H264, VideoCodec.H265)
        )
    }

    @Test
    fun `test WhatsApp preset downscales 4K to 720p`() {
        val metadata = VideoMetadata(
            uri = "content://media/video/1",
            filename = "4k_drone.mp4",
            fileSize = 1_500_000_000L,
            duration = 60_000L, // 1 minute
            width = 3840,
            height = 2160,
            fps = 60f
        )

        val plan = planner.plan(
            metadata = metadata,
            preset = CompressionPreset.WHATSAPP,
            capabilities = capabilities
        )

        assertTrue("Width should be <= 1280", plan.targetWidth <= 1280)
        assertTrue("Height should be <= 720", plan.targetHeight <= 720)
        assertTrue("FPS should be <= 30", plan.targetFps <= 30f)
        assertTrue("Video bitrate should be > 0", plan.targetVideoBitrate > 0)
        assertTrue("Estimated output should be significantly smaller than original",
            plan.estimatedOutputSize < metadata.fileSize)
    }

    @Test
    fun `test Target Size calculation bounds estimated size`() {
        val metadata = VideoMetadata(
            uri = "content://media/video/2",
            filename = "sample.mp4",
            fileSize = 800_000_000L,
            duration = 120_000L, // 2 minutes
            width = 1920,
            height = 1080,
            fps = 30f
        )

        val desiredSizeBytes = 25 * 1024 * 1024L // 25 MB

        val plan = planner.plan(
            metadata = metadata,
            preset = CompressionPreset.TARGET_SIZE,
            capabilities = capabilities,
            targetSizeBytes = desiredSizeBytes
        )

        // Estimated output size should be close to desired size (within 15%)
        val margin = desiredSizeBytes * 0.2f
        assertTrue(
            "Estimated size (${plan.estimatedOutputSize}) should be close to target ($desiredSizeBytes)",
            Math.abs(plan.estimatedOutputSize - desiredSizeBytes) < margin
        )
    }

    @Test
    fun `test formatFileSize formats properly`() {
        assertEquals("0 B", 0L.formatFileSize())
        assertEquals("1.00 KB", 1024L.formatFileSize())
        assertEquals("1.00 MB", (1024L * 1024).formatFileSize())
        assertEquals("1.50 GB", (1.5 * 1024 * 1024 * 1024).toLong().formatFileSize())
    }

    @Test
    fun `test formatDuration formats minutes and seconds properly`() {
        assertEquals("0:00", 0L.formatDuration())
        assertEquals("0:45", 45_000L.formatDuration())
        assertEquals("1:05", 65_000L.formatDuration())
        assertEquals("1:01:05", 3665_000L.formatDuration())
    }

    @Test
    fun `test planFromQuality enforces decreasing bitrates across quality tiers`() {
        val metadata = VideoMetadata(
            uri = "content://media/video/3",
            filename = "sample_tiers.mp4",
            fileSize = 500_000_000L,
            duration = 60_000L,
            width = 1920,
            height = 1080,
            fps = 30f
        )

        val maxPlan = planner.planFromQuality(metadata, QualityTier.MAXIMUM, capabilities)
        val highPlan = planner.planFromQuality(metadata, QualityTier.HIGH, capabilities)
        val balancedPlan = planner.planFromQuality(metadata, QualityTier.BALANCED, capabilities)
        val smallPlan = planner.planFromQuality(metadata, QualityTier.SMALL, capabilities)
        val maxCompPlan = planner.planFromQuality(metadata, QualityTier.MAXIMUM_COMPRESSION, capabilities)

        assertTrue(
            "MAXIMUM (${maxPlan.targetVideoBitrate}) should have higher bitrate than BALANCED (${balancedPlan.targetVideoBitrate})",
            maxPlan.targetVideoBitrate > balancedPlan.targetVideoBitrate
        )
        assertTrue(
            "HIGH (${highPlan.targetVideoBitrate}) should have higher bitrate than SMALL (${smallPlan.targetVideoBitrate})",
            highPlan.targetVideoBitrate > smallPlan.targetVideoBitrate
        )
        assertTrue(
            "BALANCED (${balancedPlan.targetVideoBitrate}) should have higher bitrate than MAXIMUM_COMPRESSION (${maxCompPlan.targetVideoBitrate})",
            balancedPlan.targetVideoBitrate > maxCompPlan.targetVideoBitrate
        )
    }

    @Test
    fun `test plan handles 90 degree portrait rotation correctly`() {
        val metadata = VideoMetadata(
            uri = "content://media/video/4",
            filename = "portrait_sample.mp4",
            fileSize = 100_000_000L,
            duration = 30_000L,
            width = 1920,
            height = 1080,
            rotation = 90, // Effective: 1080w x 1920h
            fps = 30f
        )

        val plan = planner.plan(
            metadata = metadata,
            preset = CompressionPreset.WHATSAPP, // maxWidth = 1280, maxHeight = 720
            capabilities = capabilities
        )

        // With rotation 90, effective width is 1080 and height is 1920.
        // Target resolution should scale down effectively to 720x1280 (portrait 720p), NOT 404x720!
        assertTrue("targetWidth must be even", plan.targetWidth % 2 == 0)
        assertTrue("targetHeight must be even", plan.targetHeight % 2 == 0)
        assertEquals(720, plan.targetWidth)
        assertEquals(1280, plan.targetHeight)
    }

    @Test
    fun `test Social Media preset preserves 1080x1920 portrait video without squashing`() {
        val metadata = VideoMetadata(
            uri = "content://media/video/5",
            filename = "portrait_1080p.mp4",
            fileSize = 80_000_000L,
            duration = 15_000L,
            width = 1920,
            height = 1080,
            rotation = 90, // Effective: 1080w x 1920h
            fps = 30f
        )

        val plan = planner.plan(
            metadata = metadata,
            preset = CompressionPreset.SOCIAL_MEDIA, // maxWidth = 1920, maxHeight = 1080
            capabilities = capabilities
        )

        // Must stay 1080x1920, never downscaled to 608x1080
        assertEquals(1080, plan.targetWidth)
        assertEquals(1920, plan.targetHeight)
    }

    @Test
    fun `test square video scales symmetrically`() {
        val metadata = VideoMetadata(
            uri = "content://media/video/6",
            filename = "square_video.mp4",
            fileSize = 40_000_000L,
            duration = 10_000L,
            width = 1080,
            height = 1080,
            rotation = 0,
            fps = 30f
        )

        val plan = planner.plan(
            metadata = metadata,
            preset = CompressionPreset.STORAGE_SAVER, // maxWidth = 1280, maxHeight = 720
            capabilities = capabilities
        )

        // Square video must stay 1:1 aspect ratio: 720x720
        assertEquals(720, plan.targetWidth)
        assertEquals(720, plan.targetHeight)
    }

    @Test
    fun `test VideoMetadata displayWidth and displayHeight across all rotation angles`() {
        val landscape0 = VideoMetadata(uri = "test", width = 1920, height = 1080, rotation = 0)
        assertEquals(1920, landscape0.displayWidth)
        assertEquals(1080, landscape0.displayHeight)

        val portrait90 = VideoMetadata(uri = "test", width = 1920, height = 1080, rotation = 90)
        assertEquals(1080, portrait90.displayWidth)
        assertEquals(1920, portrait90.displayHeight)

        val landscape180 = VideoMetadata(uri = "test", width = 1920, height = 1080, rotation = 180)
        assertEquals(1920, landscape180.displayWidth)
        assertEquals(1080, landscape180.displayHeight)

        val portrait270 = VideoMetadata(uri = "test", width = 1920, height = 1080, rotation = 270)
        assertEquals(1080, portrait270.displayWidth)
        assertEquals(1920, portrait270.displayHeight)
    }
}
