package com.compressflow.app.billing

/**
 * Feature gate abstraction for Free vs Pro.
 * UI only asks canUse(feature) — billing implementation is pluggable.
 */
class EntitlementManager {

    // In initial version, all features are unlocked
    // When billing is integrated, this will check purchase status
    private var _isPro: Boolean = false

    val isPro: Boolean get() = _isPro

    fun canUseFeature(feature: Feature): Boolean {
        if (_isPro) return true
        return feature.availableInFree
    }

    fun getRemainingUsage(feature: Feature): Int {
        if (_isPro) return Int.MAX_VALUE
        return feature.freeLimit
    }

    // For testing / future billing integration
    fun setPro(pro: Boolean) {
        _isPro = pro
    }
}

enum class Feature(
    val displayName: String,
    val availableInFree: Boolean,
    val freeLimit: Int = Int.MAX_VALUE
) {
    BASIC_COMPRESSION("Basic Compression", true),
    ADVANCED_SETTINGS("Advanced Settings", false),
    TARGET_SIZE("Target Size", false),
    BATCH_PROCESSING("Batch Processing", true, freeLimit = 3),
    HEVC_ENCODING("H.265/HEVC", false),
    RESOLUTION_4K("4K Output", false),
    CUSTOM_PRESETS("Custom Presets", false),
    METADATA_CONTROL("Metadata Control", false),
    NO_ADS("Ad-Free", false)
}
