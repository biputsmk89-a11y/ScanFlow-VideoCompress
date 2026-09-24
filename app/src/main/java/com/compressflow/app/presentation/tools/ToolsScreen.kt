package com.compressflow.app.presentation.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compressflow.app.core.extensions.formatBitrate
import com.compressflow.app.core.extensions.formatDuration
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.domain.model.VideoCodec
import com.compressflow.app.domain.model.VideoMetadata

// ============================================================
// CompressFlow Tools Screen — All 8 Tools Fully Functional
// ============================================================

data class ToolGridItem(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val badge: String? = null,
    val onClick: () -> Unit = {}
)

@Composable
fun ToolsScreen(
    onNavigateToStorageAnalyzer: () -> Unit = {},
    onNavigateToBatchSelect: () -> Unit = {},
    onNavigateToQualityCompare: () -> Unit = {},
    onNavigateToTrimVideo: () -> Unit = {},
    onNavigateToVideoDetail: (String) -> Unit = {},
    viewModel: ToolsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val capabilities = viewModel.capabilities

    var pendingTool by remember { mutableStateOf<ToolType?>(null) }
    var selectedCodec by remember { mutableStateOf(VideoCodec.H264) }
    var showCodecDialog by remember { mutableStateOf(false) }

    // Single shared video picker for all tools
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val tool = pendingTool
        if (uri != null && tool != null) {
            val codec = if (tool == ToolType.RE_ENCODE) selectedCodec else null
            viewModel.processVideo(uri, tool, codec)
        }
        pendingTool = null
    }

    fun launchTool(tool: ToolType) {
        pendingTool = tool
        if (tool == ToolType.RE_ENCODE) {
            showCodecDialog = true
        } else {
            videoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        }
    }

    // ── Codec Selection Dialog (for Re-encode) ──
    if (showCodecDialog) {
        CodecSelectionDialog(
            capabilities = capabilities,
            selectedCodec = selectedCodec,
            onCodecSelected = { codec ->
                selectedCodec = codec
                showCodecDialog = false
                videoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onDismiss = {
                showCodecDialog = false
                pendingTool = null
            }
        )
    }

    // ── State-driven Dialogs ──
    when (val s = state) {
        is ToolProcessingState.Processing -> {
            ProcessingDialog(
                toolName = s.toolName,
                progress = s.progress,
                onCancel = { viewModel.cancelProcessing() }
            )
        }

        is ToolProcessingState.Completed -> {
            CompletedDialog(
                state = s,
                onPlay = { viewModel.playOutput(s.outputPath, isAudio = s.toolName == "Extract Audio") },
                onShare = { viewModel.shareOutput(s.outputPath, isAudio = s.toolName == "Extract Audio") },
                onDismiss = { viewModel.resetState() }
            )
        }

        is ToolProcessingState.VideoInfo -> {
            VideoInfoDialog(
                metadata = s.metadata,
                onDismiss = { viewModel.resetState() }
            )
        }

        is ToolProcessingState.Error -> {
            ErrorDialog(
                message = s.message,
                onDismiss = { viewModel.resetState() }
            )
        }

        is ToolProcessingState.Idle -> { /* No dialog */ }
    }

    // ── Tools Grid — All 8 tools fully functional ──
    val tools = listOf(
        ToolGridItem(
            icon = Icons.Outlined.DynamicFeed,
            label = "Batch Compress",
            description = "Compress multiple videos",
            onClick = onNavigateToBatchSelect
        ),
        ToolGridItem(
            icon = Icons.Outlined.Storage,
            label = "Storage Analyzer",
            description = "Find large videos & reclaim",
            onClick = onNavigateToStorageAnalyzer
        ),
        ToolGridItem(
            icon = Icons.Outlined.Compare,
            label = "Quality Compare",
            description = "Interactive before vs after slider",
            onClick = onNavigateToQualityCompare
        ),
        ToolGridItem(
            icon = Icons.Outlined.ContentCut,
            label = "Trim Video",
            description = "Cut start/end frame-accurately",
            onClick = onNavigateToTrimVideo
        ),
        ToolGridItem(
            icon = Icons.Outlined.SwapHoriz,
            label = "Re-encode",
            description = "Change codec (H.264/H.265/AV1)",
            onClick = { launchTool(ToolType.RE_ENCODE) }
        ),
        ToolGridItem(
            icon = Icons.Outlined.MusicNote,
            label = "Extract Audio",
            description = "Save audio track as M4A",
            onClick = { launchTool(ToolType.EXTRACT_AUDIO) }
        ),
        ToolGridItem(
            icon = Icons.AutoMirrored.Outlined.VolumeOff,
            label = "Remove Audio",
            description = "Silent video output",
            onClick = { launchTool(ToolType.REMOVE_AUDIO) }
        ),
        ToolGridItem(
            icon = Icons.Outlined.DeleteSweep,
            label = "Remove Metadata",
            description = "Strip private GPS & device tags",
            onClick = { launchTool(ToolType.REMOVE_METADATA) }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Text(
            text = "Video utilities & size optimization",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tools) { tool ->
                ToolCard(tool)
            }
        }
    }
}

// ── Tool Card ─────────────────────────────────────────────────

@Composable
private fun ToolCard(tool: ToolGridItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { tool.onClick() }
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.label,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (tool.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = tool.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column {
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ── Processing Dialog ─────────────────────────────────────────

@Composable
private fun ProcessingDialog(
    toolName: String,
    progress: Float,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Can't dismiss while processing */ },
        icon = {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = toolName, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Text(
                    text = "Processing… ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

// ── Completed Dialog ──────────────────────────────────────────

@Composable
private fun CompletedDialog(
    state: ToolProcessingState.Completed,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val saveLocation = if (state.toolName == "Extract Audio") {
        "Music/CompressFlow"
    } else {
        "Movies/CompressFlow"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "${state.toolName} Complete",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Output size:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = state.outputSize.formatFileSize(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (state.originalSize > 0 && state.outputSize < state.originalSize) {
                    val savedPct = ((state.originalSize - state.outputSize).toFloat() /
                            state.originalSize * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Size reduced:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$savedPct%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Saved to $saveLocation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (state.toolName == "Extract Audio") "Play Audio" else "Play Video")
                    }

                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// ── Video Info Dialog ─────────────────────────────────────────

@Composable
private fun VideoInfoDialog(
    metadata: VideoMetadata,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = metadata.filename ?: "Video Information",
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Video Section ──
                Text(
                    "Video",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                InfoRow("Resolution", "${metadata.width}×${metadata.height}")
                InfoRow("Duration", metadata.duration.formatDuration())
                InfoRow("File Size", metadata.fileSize.formatFileSize())
                if (metadata.fps > 0) {
                    InfoRow("Frame Rate", "${metadata.fps.toInt()} FPS")
                }
                if (metadata.videoBitrate > 0) {
                    InfoRow("Video Bitrate", metadata.videoBitrate.formatBitrate())
                }
                metadata.videoCodec?.let { InfoRow("Video Codec", it) }
                InfoRow(
                    "Orientation",
                    metadata.orientation.name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                )
                if (metadata.rotation != 0) {
                    InfoRow("Rotation", "${metadata.rotation}°")
                }
                InfoRow(
                    "HDR",
                    if (metadata.isHdr) "Yes (HDR10/HLG)" else "No (SDR)"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Audio Section ──
                Text(
                    "Audio",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                InfoRow("Has Audio", if (metadata.hasAudio) "Yes" else "No")
                if (metadata.hasAudio) {
                    metadata.audioCodec?.let { InfoRow("Audio Codec", it) }
                    if (metadata.audioBitrate > 0) {
                        InfoRow("Audio Bitrate", metadata.audioBitrate.formatBitrate())
                    }
                    if (metadata.audioSampleRate > 0) {
                        InfoRow("Sample Rate", "${metadata.audioSampleRate} Hz")
                    }
                    if (metadata.audioChannels > 0) {
                        val channelLabel = when (metadata.audioChannels) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            else -> "${metadata.audioChannels} channels"
                        }
                        InfoRow("Channels", channelLabel)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Container Section ──
                Text(
                    "Container",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                metadata.mimeType?.let { InfoRow("MIME Type", it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Error Dialog ──────────────────────────────────────────────

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(text = "Error", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// ── Codec Selection Dialog ────────────────────────────────────

@Composable
private fun CodecSelectionDialog(
    capabilities: com.compressflow.app.media.capability.CapabilityDetector.DeviceCapabilities,
    selectedCodec: VideoCodec,
    onCodecSelected: (VideoCodec) -> Unit,
    onDismiss: () -> Unit
) {
    val codecs = buildList {
        add(
            Triple(
                VideoCodec.H264,
                "H.264 (AVC)",
                "Maximum compatibility, plays on all devices"
            )
        )
        if (capabilities.supportsH265) {
            add(
                Triple(
                    VideoCodec.H265,
                    "H.265 (HEVC)",
                    "~40% smaller at same quality, widely supported"
                )
            )
        }
        if (capabilities.supportsAv1) {
            add(
                Triple(
                    VideoCodec.AV1,
                    "AV1 (Next-gen)",
                    "Best compression, royalty-free open codec"
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(text = "Select Output Codec", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                codecs.forEach { (codec, label, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCodecSelected(codec) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCodec == codec,
                            onClick = { onCodecSelected(codec) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
