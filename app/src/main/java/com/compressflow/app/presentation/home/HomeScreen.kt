package com.compressflow.app.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// CompressFlow Home Screen
// UI translated from StitchMCP screens/01-home.html
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToVideoDetail: (String) -> Unit = {},
    onNavigateToBatchSelect: () -> Unit = {},
    onNavigateToStorageAnalyzer: () -> Unit = {}
) {
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onNavigateToVideoDetail(it.toString())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── App Header ──
        item {
            HomeHeader()
        }

        // ── Privacy Badge ──
        item {
            PrivacyBadge(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                onPresetClick = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )
        }

        // ── Recent Compressions ──
        item {
            RecentCompressionsSection(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // ── Quick Tools Row ──
        item {
            QuickToolsSection(
                modifier = Modifier.padding(horizontal = 16.dp),
                onBatchCompress = onNavigateToBatchSelect,
                onStorageAnalyzer = onNavigateToStorageAnalyzer
            )
        }

        // ── Device Info Card ──
        item {
            DeviceInfoCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

// ── Header ──────────────────────────────────────────────────

@Composable
private fun HomeHeader() {
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
            // App icon
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
                        letterSpacing = (-0.3).sp
                    )
                    // 100% Local badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.height(20.dp)
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
                                color = MaterialTheme.colorScheme.secondary
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

        // Profile avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Privacy Badge ───────────────────────────────────────────

@Composable
private fun PrivacyBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
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
                color = MaterialTheme.colorScheme.secondary
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
            letterSpacing = (-0.3).sp
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative background blurs (matching StitchMCP design)
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
                // Icon circle
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
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Choose from Gallery, Camera, or Files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Format badge
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

                // Browse Videos button
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
                        style = MaterialTheme.typography.labelLarge
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
    val iconTint: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
)

@Composable
private fun QuickPresetsSection(
    modifier: Modifier = Modifier,
    onPresetClick: () -> Unit = {}
) {
    val presets = listOf(
        PresetItem(Icons.AutoMirrored.Outlined.Chat, "WhatsApp", "< 16 MB or 64 MB"),
        PresetItem(Icons.Outlined.Share, "Social Media", "1080p • 30fps Crisp"),
        PresetItem(
            Icons.Outlined.Inventory2, "Storage Saver", "~70% space saved",
            iconTint = { MaterialTheme.colorScheme.secondary }
        ),
        PresetItem(Icons.Outlined.Email, "Email Friendly", "< 25 MB • Fast")
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "One-tap profiles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2x2 Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.take(2).forEach { preset ->
                    PresetCard(
                        preset = preset,
                        modifier = Modifier.weight(1f),
                        onClick = onPresetClick
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.drop(2).forEach { preset ->
                    PresetCard(
                        preset = preset,
                        modifier = Modifier.weight(1f),
                        onClick = onPresetClick
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
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
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
                    color = MaterialTheme.colorScheme.onSurface
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

// ── Recent Compressions ─────────────────────────────────────

@Composable
private fun RecentCompressionsSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View all",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Empty state
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No compressions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Your compressed videos will appear here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
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
    onBatchCompress: () -> Unit = {},
    onStorageAnalyzer: () -> Unit = {}
) {
    val tools = listOf(
        ToolItem(Icons.Outlined.DynamicFeed, "Batch", onBatchCompress),
        ToolItem(Icons.Outlined.ContentCut, "Trim"),
        ToolItem(Icons.Outlined.SwapHoriz, "Convert"),
        ToolItem(Icons.Outlined.MusicNote, "Audio"),
        ToolItem(Icons.Outlined.Storage, "Storage", onStorageAnalyzer)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Quick Tools",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Device Info Card ────────────────────────────────────────

@Composable
private fun DeviceInfoCard(modifier: Modifier = Modifier) {
    val capabilities = remember {
        com.compressflow.app.media.capability.CapabilityDetector().detect()
    }
    val deviceName = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"
    val codecStatus = buildString {
        if (capabilities.h265HardwareEncoder) append("H.265 HW") else append("H.264 HW")
        if (capabilities.supportsAv1) append(" • AV1")
        append(" • Max ${capabilities.maxSupportedHeight}p")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
