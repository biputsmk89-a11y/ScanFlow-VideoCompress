package com.compressflow.app.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.compressflow.app.core.extensions.formatDuration
import com.compressflow.app.core.extensions.formatFileSize
import com.compressflow.app.data.local.database.CompressionHistoryEntity
import com.compressflow.app.domain.model.CompressionPreset
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ============================================================
// CompressFlow Home Screen — 100% Functional & Non-Gimmick
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToVideoDetail: (String) -> Unit = {},
    onNavigateToBatchSelect: () -> Unit = {},
    onNavigateToStorageAnalyzer: () -> Unit = {},
    onNavigateToQualityCompare: () -> Unit = {},
    onNavigateToTrimVideo: () -> Unit = {},
    onNavigateToTools: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val recentHistory by viewModel.recentHistory.collectAsState()
    val capabilities = viewModel.capabilities

    var showHardwareDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onNavigateToVideoDetail(it.toString())
        }
    }

    // ── Privacy & Offline Engine Info Modal ──
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "100% Offline & Private",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "CompressFlow runs entirely on your phone using native Android MediaCodec hardware encoders. No videos, metadata, or telemetry are ever uploaded to cloud servers. No internet connection is used or required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // ── Hardware Engine Details Modal ──
    if (showHardwareDialog) {
        AlertDialog(
            onDismissRequest = { showHardwareDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Device Encoding Hardware",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("• H.264 Encoder: ${if (capabilities.supportsH264) "Hardware Accelerated" else "Software"}")
                    Text("• H.265/HEVC Encoder: ${if (capabilities.h265HardwareEncoder) "Hardware Accelerated" else if (capabilities.supportsH265) "Available" else "Not Supported"}")
                    Text("• AV1 Support: ${if (capabilities.supportsAv1) "Hardware Supported" else "Auto-fallback to HEVC/H.264"}")
                    Text("• Max Resolution: ${capabilities.maxSupportedWidth}×${capabilities.maxSupportedHeight}")
                    Text("• Max Frame Rate: ${capabilities.maxSupportedFrameRate} FPS")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Engine: Google Media3 Transformer (Zero Cloud)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHardwareDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── App Header ──
        item {
            HomeHeader(
                onAvatarClick = onNavigateToSettings,
                onLocalBadgeClick = { showPrivacyDialog = true }
            )
        }

        // ── Privacy Badge ──
        item {
            PrivacyBadge(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { showPrivacyDialog = true }
            )
        }

        // ── Hero Tagline ──
        item {
            HeroSection(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Primary CTA Card ──
        item {
            PrimaryCTACard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                onBrowseVideos = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )
        }

        // ── Quick Presets Grid ──
        item {
            QuickPresetsSection(
                modifier = Modifier.padding(horizontal = 16.dp),
                onPresetClick = { preset ->
                    viewModel.onPresetSelected(preset)
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )
        }

        // ── Recent Compressions ──
        item {
            RecentCompressionsSection(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                recentList = recentHistory,
                onViewAll = onNavigateToHistory,
                onPlayItem = { viewModel.playVideo(it) }
            )
        }

        // ── Quick Tools Row ──
        item {
            QuickToolsSection(
                modifier = Modifier.padding(horizontal = 16.dp),
                onTrimVideo = onNavigateToTrimVideo,
                onBatchCompress = onNavigateToBatchSelect,
                onStorageAnalyzer = onNavigateToStorageAnalyzer,
                onQualityCompare = onNavigateToQualityCompare,
                onNavigateToTools = onNavigateToTools
            )
        }

        // ── Device Info Card ──
        item {
            DeviceInfoCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                capabilities = capabilities,
                onCardClick = { showHardwareDialog = true }
            )
        }
    }
}

// ── Header ──────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    onAvatarClick: () -> Unit = {},
    onLocalBadgeClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.compressflow.app.presentation.components.CompressFlowLogo(size = 36.dp)

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CompressFlow",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 100% Local badge (clickable)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .height(20.dp)
                            .clickable(onClick = onLocalBadgeClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Text(
                                text = "100% Local",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Profile avatar (clickable -> Settings)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Settings Profile",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Privacy Badge ───────────────────────────────────────────

@Composable
private fun PrivacyBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Text(
                text = "Offline & Private • On-device engine",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Hero Section ────────────────────────────────────────────

@Composable
private fun HeroSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Make videos smaller.\nKeep them better.",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.3).sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hardware-accelerated processing entirely on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Primary CTA Card ────────────────────────────────────────

@Composable
private fun PrimaryCTACard(
    modifier: Modifier = Modifier,
    onBrowseVideos: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onBrowseVideos),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .offset(x = 220.dp, y = (-48).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .blur(40.dp)
            )
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .offset(x = (-40).dp, y = 120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                    .blur(40.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Video",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Choose from Gallery, Camera, or Files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "MP4, MOV, MKV up to 4K",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onBrowseVideos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Browse Videos",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Quick Presets ────────────────────────────────────────────

data class PresetItem(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val targetPreset: CompressionPreset,
    val iconTint: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
)

@Composable
private fun QuickPresetsSection(
    modifier: Modifier = Modifier,
    onPresetClick: (CompressionPreset) -> Unit = {}
) {
    val presets = listOf(
        PresetItem(
            Icons.AutoMirrored.Outlined.Chat,
            "WhatsApp",
            "< 16 MB or 64 MB",
            CompressionPreset.WHATSAPP
        ),
        PresetItem(
            Icons.Outlined.Share,
            "Social Media",
            "1080p • 30fps Crisp",
            CompressionPreset.SOCIAL_MEDIA
        ),
        PresetItem(
            Icons.Outlined.Inventory2,
            "Storage Saver",
            "~70% space saved",
            CompressionPreset.STORAGE_SAVER,
            iconTint = { MaterialTheme.colorScheme.secondary }
        ),
        PresetItem(
            Icons.Outlined.Email,
            "Email Friendly",
            "< 25 MB • Fast",
            CompressionPreset.EMAIL
        )
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "One-tap profiles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.take(2).forEach { preset ->
                    PresetCard(
                        preset = preset,
                        modifier = Modifier.weight(1f),
                        onClick = { onPresetClick(preset.targetPreset) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.drop(2).forEach { preset ->
                    PresetCard(
                        preset = preset,
                        modifier = Modifier.weight(1f),
                        onClick = { onPresetClick(preset.targetPreset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: PresetItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(128.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = preset.label,
                        modifier = Modifier.size(20.dp),
                        tint = preset.iconTint()
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            Column {
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Recent Compressions (Room Database Reactive) ────────────

@Composable
private fun RecentCompressionsSection(
    modifier: Modifier = Modifier,
    recentList: List<CompressionHistoryEntity>,
    onViewAll: () -> Unit,
    onPlayItem: (CompressionHistoryEntity) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View all",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }

        if (recentList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No compressions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Your compressed videos will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentList.forEach { item ->
                    RecentHistoryCard(item = item, onClick = { onPlayItem(item) })
                }
            }
        }
    }
}

@Composable
private fun RecentHistoryCard(
    item: CompressionHistoryEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                val imageModel: Any = if (item.outputPath.startsWith("content://")) {
                    Uri.parse(item.outputPath)
                } else {
                    File(item.outputPath)
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.filename,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Duration chip
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = item.durationMs.formatDuration(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${item.originalSize.formatFileSize()} ➔ ${item.compressedSize.formatFileSize()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Savings chip
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "-${item.savedPercentage.roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Quick Tools ─────────────────────────────────────────────

data class ToolItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit = {}
)

@Composable
private fun QuickToolsSection(
    modifier: Modifier = Modifier,
    onTrimVideo: () -> Unit = {},
    onBatchCompress: () -> Unit = {},
    onStorageAnalyzer: () -> Unit = {},
    onQualityCompare: () -> Unit = {},
    onNavigateToTools: () -> Unit = {}
) {
    val tools = listOf(
        ToolItem(Icons.Outlined.ContentCut, "Trim", onTrimVideo),
        ToolItem(Icons.Outlined.DynamicFeed, "Batch", onBatchCompress),
        ToolItem(Icons.Outlined.Storage, "Storage", onStorageAnalyzer),
        ToolItem(Icons.Outlined.Compare, "Compare", onQualityCompare),
        ToolItem(Icons.Outlined.Build, "All Tools", onNavigateToTools)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Quick Tools",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools) { tool ->
                ToolChip(tool)
            }
        }
    }
}

@Composable
private fun ToolChip(tool: ToolItem) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = tool.onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Device Info Card ────────────────────────────────────────

@Composable
private fun DeviceInfoCard(
    modifier: Modifier = Modifier,
    capabilities: com.compressflow.app.media.capability.CapabilityDetector.DeviceCapabilities,
    onCardClick: () -> Unit = {}
) {
    val deviceName = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"
    val codecStatus = buildString {
        if (capabilities.h265HardwareEncoder) append("H.265 HW") else append("H.264 HW")
        if (capabilities.supportsAv1) append(" • AV1")
        append(" • Max ${capabilities.maxSupportedHeight}p")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = codecStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (capabilities.h265HardwareEncoder) "HW Ready" else "Ready",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
