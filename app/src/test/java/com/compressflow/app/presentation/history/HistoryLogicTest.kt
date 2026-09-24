package com.compressflow.app.presentation.history

import com.compressflow.app.core.extensions.formatDuration
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import org.junit.Assert.*
import org.junit.Test

class HistoryLogicTest {

    private val sampleList = listOf(
        CompressionHistoryEntity(
            id = 1,
            filename = "holiday_trip.mp4",
            originalSize = 100_000_000L,
            compressedSize = 25_000_000L,
            savedPercentage = 75f,
            durationMs = 60_000L,
            resolution = "1920×1080",
            codec = "H.265",
            outputPath = "/storage/Movies/CompressFlow/holiday_trip.mp4",
            timestamp = 1000L
        ),
        CompressionHistoryEntity(
            id = 2,
            filename = "meeting_audio.m4a",
            originalSize = 50_000_000L,
            compressedSize = 5_000_000L,
            savedPercentage = 90f,
            durationMs = 120_000L,
            resolution = "Audio Track",
            codec = "M4A/AAC",
            outputPath = "/storage/Music/CompressFlow/meeting_audio.m4a",
            timestamp = 2000L
        ),
        CompressionHistoryEntity(
            id = 3,
            filename = "family_dinner.mp4",
            originalSize = 200_000_000L,
            compressedSize = 120_000_000L,
            savedPercentage = 40f,
            durationMs = 180_000L,
            resolution = "1280×720",
            codec = "H.264",
            outputPath = "/storage/Movies/CompressFlow/family_dinner.mp4",
            timestamp = 3000L
        )
    )

    @Test
    fun `test search filtering by filename and codec`() {
        val tripMatches = sampleList.filter { it.filename.contains("holiday", ignoreCase = true) }
        assertEquals(1, tripMatches.size)
        assertEquals("holiday_trip.mp4", tripMatches.first().filename)

        val h264Matches = sampleList.filter { it.codec.contains("H.264", ignoreCase = true) }
        assertEquals(1, h264Matches.size)
        assertEquals("family_dinner.mp4", h264Matches.first().filename)
    }

    @Test
    fun `test filter by video and audio type`() {
        fun isAudio(item: CompressionHistoryEntity): Boolean {
            return item.filename.endsWith(".m4a", true) ||
                    item.resolution.contains("audio", true) ||
                    item.codec.contains("audio", true)
        }

        val videos = sampleList.filter { !isAudio(it) }
        val audios = sampleList.filter { isAudio(it) }

        assertEquals(2, videos.size)
        assertEquals(1, audios.size)
        assertEquals("meeting_audio.m4a", audios.first().filename)
    }

    @Test
    fun `test sorting options`() {
        val sortedNewest = sampleList.sortedByDescending { it.timestamp }
        assertEquals(3L, sortedNewest.first().id) // timestamp 3000

        val sortedSavings = sampleList.sortedByDescending { it.savedPercentage }
        assertEquals(2L, sortedSavings.first().id) // 90% savings

        val sortedSmallest = sampleList.sortedBy { it.compressedSize }
        assertEquals(2L, sortedSmallest.first().id) // 5 MB
    }

    @Test
    fun `test history statistics calculation`() {
        val count = sampleList.size
        val totalSavedBytes = sampleList.sumOf { (it.originalSize - it.compressedSize).coerceAtLeast(0L) }
        val avgPercentage = sampleList.map { it.savedPercentage }.average().toFloat()

        assertEquals(3, count)
        // (100-25) + (50-5) + (200-120) = 75 + 45 + 80 = 200 MB
        assertEquals(200_000_000L, totalSavedBytes)
        // (75 + 90 + 40) / 3 = 205 / 3 = 68.33%
        assertEquals(68.33f, avgPercentage, 0.1f)
    }

    @Test
    fun `test duration and file size formatting`() {
        val durationMs = 125_000L // 2m 5s
        assertEquals("2:05", durationMs.formatDuration())

        val sizeBytes = 200_000_000L
        assertEquals("191 MB", sizeBytes.formatFileSize())
    }
}
