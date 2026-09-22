package com.compressflow.app.media

import com.compressflow.app.domain.model.VideoCodec
import com.compressflow.app.media.capability.CapabilityDetector
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CapabilityDetectorTest {

    private lateinit var detector: CapabilityDetector

    @Before
    fun setup() {
        detector = CapabilityDetector()
    }

    @Test
    fun `test bestCodec prefers H265 when hardware HEVC is available`() {
        val caps = CapabilityDetector.DeviceCapabilities(
            supportsH264 = true,
            supportsH265 = true,
            h264HardwareEncoder = true,
            h265HardwareEncoder = true
        )
        assertEquals(VideoCodec.H265, detector.bestCodec(caps))
    }

    @Test
    fun `test bestCodec falls back to H264 when HEVC is unsupported`() {
        val caps = CapabilityDetector.DeviceCapabilities(
            supportsH264 = true,
            supportsH265 = false,
            h264HardwareEncoder = true,
            h265HardwareEncoder = false
        )
        assertEquals(VideoCodec.H264, detector.bestCodec(caps))
    }

    @Test
    fun `test bestCodec defaults to H264 on minimal device`() {
        val caps = CapabilityDetector.DeviceCapabilities(
            supportsH264 = true,
            supportsH265 = false,
            h264HardwareEncoder = false,
            h265HardwareEncoder = false
        )
        assertEquals(VideoCodec.H264, detector.bestCodec(caps))
    }
}
