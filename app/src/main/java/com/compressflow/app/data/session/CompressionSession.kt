package com.compressflow.app.data.session

import android.net.Uri
import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.CompressionResult
import com.compressflow.app.domain.model.VideoMetadata
import java.io.File

/**
 * In-memory session manager holding active video compression state
 * across VideoDetail, CompressionProgress, and Result screens.
 */
object CompressionSession {
    var currentUri: Uri? = null
    var currentMetadata: VideoMetadata? = null
    var currentPlan: CompressionPlan? = null
    var currentPreset: com.compressflow.app.domain.model.CompressionPreset = com.compressflow.app.domain.model.CompressionPreset.SOCIAL_MEDIA
    var currentTargetSizeMb: Int = 25
    var outputFile: File? = null
    var lastResult: CompressionResult? = null

    // Batch queue support
    var batchUris: List<Uri> = emptyList()
    val batchResults: MutableList<CompressionResult> = mutableListOf()

    fun reset() {
        currentUri = null
        currentMetadata = null
        currentPlan = null
        currentPreset = com.compressflow.app.domain.model.CompressionPreset.SOCIAL_MEDIA
        currentTargetSizeMb = 25
        outputFile = null
        lastResult = null
        batchUris = emptyList()
        batchResults.clear()
    }
}
