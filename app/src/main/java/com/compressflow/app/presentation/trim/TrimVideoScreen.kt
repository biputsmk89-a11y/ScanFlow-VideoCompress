@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.compressflow.app.presentation.trim

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.compressflow.app.core.extensions.formatDuration
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.data.session.CompressionSession
import com.compressflow.app.domain.usecase.TrimExecutionResult

@Composable
fun TrimVideoScreen(
    initialVideoUri: String? = null,
    onNavigateBack: () -> Unit = {},
    onContinueToCompress: (String) -> Unit = {},
    viewModel: TrimVideoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadVideo(it) }
    }

    LaunchedEffect(initialVideoUri) {
        if (!initialVideoUri.isNullOrBlank()) {
            viewModel.loadVideo(Uri.parse(initialVideoUri))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trim Video",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.videoUri != null) {
                        TextButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        ) {
                            Text("Change Video")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView()
                }

                uiState.videoUri == null -> {
                    EmptyVideoState(
                        onSelectVideo = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )
                }

                else -> {
                    TrimEditorContent(
                        uiState = uiState,
                        player = viewModel.player,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onPreviewSelection = { viewModel.previewSelection() },
                        onStartChanged = { viewModel.setStartMs(it) },
                        onEndChanged = { viewModel.setEndMs(it) },
                        onCompressAlsoChanged = { viewModel.setCompressAlso(it) },
                        onTrimClick = { viewModel.startTrim() }
                    )
                }
            }

            // ── Processing Modal ──
            if (uiState.isProcessing) {
                ProcessingTrimDialog(
                    progress = uiState.progress,
                    onCancel = { viewModel.cancelTrim() }
                )
            }

            // ── Result Modal ──
            uiState.result?.let { res ->
                TrimResultDialog(
                    result = res,
                    onPlay = { viewModel.playOutput(res.outputUri) },
                    onShare = { viewModel.shareOutput(res.outputUri) },
                    onCompressFurther = {
                        viewModel.clearResult()
                        onContinueToCompress(res.outputUri)
                    },
                    onDismiss = { viewModel.clearResult() }
                )
            }

            // ── Error Dialog ──
            uiState.error?.let { err ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = { Text("Trim Error", fontWeight = FontWeight.Bold) },
                    text = { Text(err, style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

// ── Editor Content ─────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun TrimEditorContent(
    uiState: TrimUiState,
    player: androidx.media3.exoplayer.ExoPlayer?,
    onTogglePlayPause: () -> Unit,
    onPreviewSelection: () -> Unit,
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
    onCompressAlsoChanged: (Boolean) -> Unit,
    onTrimClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Video Player Container ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (player != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Play / Pause Overlay Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Time Indicator (Current / Total)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${uiState.currentPositionMs.formatDuration()} / ${uiState.durationMs.formatDuration()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Selection Indicator Badge
                if (uiState.isPreviewingSelection) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "PREVIEWING SELECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // ── 2. Timeline & Range Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trim Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Selected: ${uiState.selectedDurationMs.formatDuration()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Range Slider
                if (uiState.durationMs > 0) {
                    val sliderRange = 0f..uiState.durationMs.toFloat()
                    val currentValues = uiState.startMs.toFloat()..uiState.endMs.toFloat()

                    RangeSlider(
                        value = currentValues,
                        onValueChange = { range ->
                            val newStart = range.start.toLong()
                            val newEnd = range.endInclusive.toLong()
                            if (newStart != uiState.startMs) {
                                onStartChanged(newStart)
                            }
                            if (newEnd != uiState.endMs) {
                                onEndChanged(newEnd)
                            }
                        },
                        valueRange = sliderRange,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }

                // Start & End Time Chips with Quick Adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeBox(
                        title = "Start Time",
                        timeMs = uiState.startMs,
                        onMinus = { onStartChanged(uiState.startMs - 1000L) },
                        onPlus = { onStartChanged(uiState.startMs + 1000L) },
                        modifier = Modifier.weight(1f)
                    )

                    TimeBox(
                        title = "End Time",
                        timeMs = uiState.endMs,
                        onMinus = { onEndChanged(uiState.endMs - 1000L) },
                        onPlus = { onEndChanged(uiState.endMs + 1000L) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── 3. Pipeline Option: Trim + Compress ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCompressAlsoChanged(!uiState.compressAlso) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trim & Compress (Single Pass)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Cuts duration and optimizes bitrate simultaneously",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.compressAlso,
                    onCheckedChange = onCompressAlsoChanged
                )
            }
        }

        // ── 4. Action Buttons ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPreviewSelection,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Preview Cut",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Button(
                onClick = onTrimClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCut,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (uiState.compressAlso) "Trim & Compress" else "Trim Video",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

// ── Time Box Component ─────────────────────────────────────────

@Composable
private fun TimeBox(
    title: String,
    timeMs: Long,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeMs.formatDuration(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onMinus,
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape
                ) {
                    Text("-1s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(
                    onClick = onPlus,
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape
                ) {
                    Text("+1s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Empty State ────────────────────────────────────────────────

@Composable
private fun EmptyVideoState(onSelectVideo: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCut,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Select a Video to Trim",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose any video from your gallery to cut the start or end frame-accurately.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSelectVideo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Video", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Loading View ───────────────────────────────────────────────

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Preparing video timeline...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Processing Dialog ──────────────────────────────────────────

@Composable
private fun ProcessingTrimDialog(
    progress: Float,
    onCancel: () -> Unit
) {
    val pct = (progress * 100).toInt()
    AlertDialog(
        onDismissRequest = {},
        icon = {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Trimming Video...",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "$pct% processed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
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

// ── Result Dialog ──────────────────────────────────────────────

@Composable
private fun TrimResultDialog(
    result: TrimExecutionResult,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onCompressFurther: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text("Trim Complete", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultRow("Original Duration:", result.originalDurationMs.formatDuration())
                ResultRow("Trimmed Duration:", result.trimmedDurationMs.formatDuration())
                ResultRow("Start Point:", result.startMs.formatDuration())
                ResultRow("End Point:", result.endMs.formatDuration())

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ResultRow("Original Size:", result.originalSize.formatFileSize())
                ResultRow("Output Size:", result.outputSize.formatFileSize())
                ResultRow("Resolution:", result.resolution)

                if (result.originalSize > result.outputSize) {
                    val saved = ((result.originalSize - result.outputSize).toFloat() / result.originalSize * 100).toInt()
                    ResultRow("Size Saved:", "$saved% reduction", isHighlight = true)
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
                        text = "Saved to Movies/CompressFlow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }

                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }

                Button(
                    onClick = onCompressFurther,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compress This Trimmed Video")
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

@Composable
private fun ResultRow(label: String, value: String, isHighlight: Boolean = false) {
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
            color = if (isHighlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
        )
    }
}
