package com.compressflow.app.presentation.video_detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compressflow.app.data.session.CompressionSession
import com.compressflow.app.domain.model.CompressionPlan
import com.compressflow.app.domain.model.CompressionPreset
import com.compressflow.app.domain.model.VideoMetadata
import com.compressflow.app.media.analyzer.VideoAnalyzer
import com.compressflow.app.media.capability.CapabilityDetector
import com.compressflow.app.media.planner.CompressionPlanner
import com.compressflow.app.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GoalType(val title: String, val description: String, val preset: CompressionPreset) {
    WHATSAPP("Send on WhatsApp", "Optimized for messaging limits without noticeable quality drop", CompressionPreset.WHATSAPP),
    SOCIAL_MEDIA("Social Media", "Crisp 1080p 30 FPS tuned for Instagram, TikTok & YouTube", CompressionPreset.SOCIAL_MEDIA),
    STORAGE_SAVER("Storage Saver", "Aggressive size reduction to free up internal phone storage", CompressionPreset.STORAGE_SAVER),
    EMAIL("Email Attachment", "Keeps file strictly under standard 25 MB inbox limits", CompressionPreset.EMAIL),
    TARGET_SIZE("Target Size", "Specify the exact desired megabytes (e.g. 50 MB, 100 MB)", CompressionPreset.TARGET_SIZE),
    ADVANCED("Advanced Settings", "For creators: manual codec (H.265/AV1), bitrate, resolution & FPS", CompressionPreset.CUSTOM)
}

data class VideoDetailUiState(
    val isLoading: Boolean = true,
    val metadata: VideoMetadata? = null,
    val selectedGoal: GoalType = GoalType.WHATSAPP,
    val targetSizeMb: Int = 50,
    val currentPlan: CompressionPlan? = null,
    val estimatedDurationSec: Float = 4.2f,
    val error: String? = null
)

class VideoDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val videoAnalyzer = VideoAnalyzer(application)
    private val capabilityDetector = CapabilityDetector()
    private val compressionPlanner = CompressionPlanner()
    private val capabilities = capabilityDetector.detect()
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    fun loadVideo(videoUriString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val uri = Uri.parse(videoUriString)
            val result = videoAnalyzer.analyze(uri)
            result.onSuccess { meta ->
                CompressionSession.currentUri = uri
                CompressionSession.currentMetadata = meta

                val appSettings = settingsRepository.settingsFlow.first()
                val defaultPresetFromSettings = when (appSettings.defaultQuality) {
                    "High Quality" -> GoalType.SOCIAL_MEDIA
                    "Maximum Space Saving" -> GoalType.STORAGE_SAVER
                    else -> GoalType.WHATSAPP
                }
                val initialGoal = GoalType.entries.find { it.preset == CompressionSession.currentPreset } ?: defaultPresetFromSettings
                var plan = compressionPlanner.plan(
                    metadata = meta,
                    preset = initialGoal.preset,
                    capabilities = capabilities
                )

                when (appSettings.defaultCodec) {
                    "H.265 / HEVC (Best compression)" -> if (capabilities.supportsH265) plan = plan.copy(videoCodec = com.compressflow.app.domain.model.VideoCodec.H265)
                    "H.264 (Maximum compatibility)" -> plan = plan.copy(videoCodec = com.compressflow.app.domain.model.VideoCodec.H264)
                    "AV1 (Next-gen)" -> if (capabilities.supportsAv1) plan = plan.copy(videoCodec = com.compressflow.app.domain.model.VideoCodec.AV1)
                }

                if (!appSettings.keepAudio) {
                    plan = plan.copy(removeAudio = true)
                }
                CompressionSession.currentPlan = plan
                CompressionSession.currentPreset = initialGoal.preset

                val estDuration = (meta.duration / 1000f * 0.15f).coerceAtLeast(2f)

                _uiState.value = VideoDetailUiState(
                    isLoading = false,
                    metadata = meta,
                    selectedGoal = initialGoal,
                    currentPlan = plan,
                    estimatedDurationSec = estDuration
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = err.message ?: "Failed to read video"
                )
            }
        }
    }

    fun selectGoal(goal: GoalType) {
        val meta = _uiState.value.metadata ?: return
        val plan = if (goal == GoalType.TARGET_SIZE) {
            val targetBytes = _uiState.value.targetSizeMb * 1024L * 1024L
            compressionPlanner.plan(
                metadata = meta,
                preset = goal.preset,
                capabilities = capabilities,
                targetSizeBytes = targetBytes
            )
        } else {
            compressionPlanner.plan(
                metadata = meta,
                preset = goal.preset,
                capabilities = capabilities
            )
        }
        CompressionSession.currentPlan = plan
        CompressionSession.currentPreset = goal.preset

        val estDuration = (meta.duration / 1000f * 0.15f).coerceAtLeast(2f)

        _uiState.value = _uiState.value.copy(
            selectedGoal = goal,
            currentPlan = plan,
            estimatedDurationSec = estDuration
        )
    }

    fun updateTargetSizeMb(mb: Int) {
        val meta = _uiState.value.metadata ?: return
        val targetBytes = mb * 1024L * 1024L
        val plan = compressionPlanner.plan(
            metadata = meta,
            preset = CompressionPreset.TARGET_SIZE,
            capabilities = capabilities,
            targetSizeBytes = targetBytes
        )
        CompressionSession.currentPlan = plan
        CompressionSession.currentTargetSizeMb = mb
        _uiState.value = _uiState.value.copy(
            targetSizeMb = mb,
            currentPlan = plan
        )
    }
}
