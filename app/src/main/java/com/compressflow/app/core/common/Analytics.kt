package com.compressflow.app.core.common

/**
 * Analytics abstraction — no vendor hardcoded.
 * Implementations can be Firebase, Mixpanel, or no-op.
 */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

sealed class AnalyticsEvent(val name: String) {
    // Never collect media content, filenames with PII, or location
    data object VideoSelected : AnalyticsEvent("video_selected")
    data class CompressionStarted(val preset: String) : AnalyticsEvent("compression_started")
    data object CompressionCompleted : AnalyticsEvent("compression_completed")
    data class CompressionFailed(val reason: String) : AnalyticsEvent("compression_failed")
    data class PresetSelected(val preset: String) : AnalyticsEvent("preset_selected")
    data object BatchStarted : AnalyticsEvent("batch_started")
    data object BatchCompleted : AnalyticsEvent("batch_completed")
    data object ShareClicked : AnalyticsEvent("share_clicked")
    data object ProScreenViewed : AnalyticsEvent("pro_screen_viewed")
}

/**
 * No-op implementation for initial release.
 */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        // Intentionally empty — privacy-first
    }
}
