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
    var outputFile: File? = null
    var lastResult: CompressionResult? = null

    fun reset() {
        currentUri = null
        currentMetadata = null
        currentPlan = null
        outputFile = null
        lastResult = null
    }
}
