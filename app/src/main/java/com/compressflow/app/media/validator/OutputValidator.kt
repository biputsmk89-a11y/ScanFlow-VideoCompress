package com.compressflow.app.media.validator

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File

/**
 * Validates compression output integrity.
 * Ensures output file is valid before presenting success.
 */
class OutputValidator(private val context: Context) {

    data class ValidationResult(
        val isValid: Boolean,
        val fileExists: Boolean = false,
        val hasSize: Boolean = false,
        val isReadable: Boolean = false,
        val hasVideoTrack: Boolean = false,
        val hasAudioTrack: Boolean = false,
        val hasDuration: Boolean = false,
        val errorMessage: String? = null
    )

    fun validate(outputFile: File, expectAudio: Boolean = true): ValidationResult {
        // 1. File exists
        if (!outputFile.exists()) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Output file does not exist"
            )
        }

        // 2. File size > 0
        if (outputFile.length() <= 0) {
            return ValidationResult(
                isValid = false,
                fileExists = true,
                errorMessage = "Output file is empty"
            )
        }

        // 3. Verify media container is readable
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(outputFile.absolutePath)

            var hasVideo = false
            var hasAudio = false
            var duration = 0L

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/")) {
                    hasVideo = true
                    duration = try {
                        format.getLong(MediaFormat.KEY_DURATION) / 1000 // to ms
                    } catch (_: Exception) { 0L }
                } else if (mime.startsWith("audio/")) {
                    hasAudio = true
                }
            }

            val hasDuration = duration > 0

            // Audio check only if expected
            val audioOk = !expectAudio || hasAudio

            return ValidationResult(
                isValid = hasVideo && audioOk && hasDuration,
                fileExists = true,
                hasSize = true,
                isReadable = true,
                hasVideoTrack = hasVideo,
                hasAudioTrack = hasAudio,
                hasDuration = hasDuration,
                errorMessage = when {
                    !hasVideo -> "No video track found in output"
                    expectAudio && !hasAudio -> "Expected audio track not found"
                    !hasDuration -> "Output has zero duration"
                    else -> null
                }
            )
        } catch (e: Exception) {
            return ValidationResult(
                isValid = false,
                fileExists = true,
                hasSize = true,
                isReadable = false,
                errorMessage = "Cannot read output file: ${e.message}"
            )
        } finally {
            extractor.release()
        }
    }
}
