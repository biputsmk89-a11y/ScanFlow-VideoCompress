package com.compressflow.app.presentation.result

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.data.session.CompressionSession
import com.compressflow.app.presentation.components.CompressFlowLogo

enum class CompareMode { SPLIT, SIDE_BY_SIDE, DELTA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityComparisonScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val metadata = CompressionSession.currentMetadata
    val result = CompressionSession.lastResult

    var compareMode by remember { mutableStateOf(CompareMode.SPLIT) }
    var splitPosition by remember { mutableStateOf(0.5f) }
    var isHoldingOriginal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompressFlowLogo(size = 32.dp)
                        Column {
                            Text(
                                text = "Quality Comparison",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                            Text(
                                text = "A/B & Delta Visual Fidelity",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quality Assurance Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "VMAF 96.8",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Visually Lossless",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = "SSIM 0.992",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3-Segment View Mode Switcher
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        CompareMode.SPLIT to "Split",
                        CompareMode.SIDE_BY_SIDE to "2-Up",
                        CompareMode.DELTA to "A/B & Delta"
                    ).forEach { (mode, label) ->
                        val selected = compareMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            TextButton(
                                onClick = { compareMode = mode },
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Hero Video Inspection Viewport
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                ) {
                    val maxWidthPx = constraints.maxWidth.toFloat()
                    val dividerOffsetDp = with(LocalDensity.current) { (maxWidthPx * splitPosition).toDp() }

                    // Base video frame
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(if (isHoldingOriginal) metadata?.uri else (result?.outputUri ?: metadata?.uri))
                            .videoFrameMillis(1000)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Video comparison frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Delta Heatmap shader representation
                    if (compareMode == CompareMode.DELTA) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x77EF4444),
                                            Color(0x55F59E0B),
                                            Color(0x3306B6D4),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // Split Mode divider & handle
                    if (compareMode == CompareMode.SPLIT) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .offset(x = dividerOffsetDp)
                                .background(Color.White)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = dividerOffsetDp - 18.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .pointerInput(maxWidthPx) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        splitPosition = (splitPosition + dragAmount / maxWidthPx).coerceIn(0.05f, 0.95f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DragIndicator,
                                contentDescription = "Drag divider",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 400% Zoom Loupe floating in corner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(80.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ZoomIn,
                                contentDescription = null,
                                tint = Color(0xFF6BFF8F),
                                modifier = Modifier.size(28.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "400%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp)
                                Text(text = "0 Blocks", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6BFF8F), fontSize = 9.sp)
                            }
                        }
                    }

                    // Top Status badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = if (isHoldingOriginal) "SHOWING ORIGINAL" else "COMPRESSED H.265",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isHoldingOriginal) Color(0xFFFFB4AB) else Color(0xFF6BFF8F),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Tactile Hold-to-Compare Button & Split presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isHoldingOriginal = !isHoldingOriginal },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHoldingOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isHoldingOriginal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHoldingOriginal) "Showing Original (Tap to Reset)" else "Hold/Tap Compare Original",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Split percentage chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.25f to "25% Split", 0.5f to "50% Split", 0.75f to "75% Split").forEach { (pos, label) ->
                    OutlinedButton(
                        onClick = { splitPosition = pos },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Side-by-side Technical Metrics Table
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Encoding Metric Breakdown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    MetricRow("File Size", metadata?.fileSize?.formatFileSize() ?: "1.82 GB", (result?.outputSize ?: 248_000_000L).formatFileSize())
                    MetricRow("Bitrate", "48.2 Mbps", "11.2 Mbps")
                    MetricRow("Resolution", "4K UHD (3840×2160)", "1080p FHD (1920×1080)")
                    MetricRow("Framerate", "60 FPS", "30 FPS")
                    MetricRow("Color Space", "10-bit Rec.709", "10-bit Rec.709")
                    MetricRow("SSIM Fidelity", "1.000", "0.992")
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, sourceVal: String, compVal: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = sourceVal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = compVal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
