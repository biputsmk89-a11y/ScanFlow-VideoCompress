package com.compressflow.app.core.extensions

import java.util.Locale

/**
 * Format file size to human-readable string.
 * Examples: "1.24 GB", "480 MB", "12.3 KB"
 */
fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    val value = this / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.${if (value >= 100) 0 else if (value >= 10) 1 else 2}f %s", value, units[digitGroups])
}

/**
 * Format duration in milliseconds to human-readable string.
 * Examples: "1:23:45", "12:34", "0:45"
 */
fun Long.formatDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/**
 * Format resolution as a readable string.
 * Examples: "4K", "1080p", "720p", "480p"
 */
fun formatResolution(width: Int, height: Int): String {
    val maxDim = maxOf(width, height)
    return when {
        maxDim >= 3840 -> "4K"
        maxDim >= 2560 -> "2K"
        maxDim >= 1920 -> "1080p"
        maxDim >= 1280 -> "720p"
        maxDim >= 854 -> "480p"
        maxDim >= 640 -> "360p"
        else -> "${width}×${height}"
    }
}

/**
 * Format compression ratio as percentage saved.
 * Example: "86%"
 */
fun formatSavedPercentage(original: Long, compressed: Long): String {
    if (original <= 0) return "0%"
    val saved = ((original - compressed).toFloat() / original * 100).toInt()
    return "$saved%"
}

/**
 * Format bitrate to readable string.
 * Examples: "12.5 Mbps", "320 Kbps"
 */
fun Long.formatBitrate(): String {
    return when {
        this >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", this / 1_000_000.0)
        this >= 1_000 -> String.format(Locale.US, "%d Kbps", this / 1000)
        else -> "$this bps"
    }
}
