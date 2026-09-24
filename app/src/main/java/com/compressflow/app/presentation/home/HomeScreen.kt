package com.compressflow.app.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val profilePhotoPath by viewModel.profilePhotoPath.collectAsState()
    val profilePhotoVersion by viewModel.profilePhotoVersion.collectAsState()

    var showHardwareDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onNavigateToVideoDetail(it.toString())
        }
    }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveProfilePhoto(it)
        }
    }

    // ── User Profile Photo Dialog ──
    if (showProfileDialog) {
        UserProfileDialog(
            profilePhotoPath = profilePhotoPath,
            profilePhotoVersion = profilePhotoVersion,
            onDismiss = { showProfileDialog = false },
            onPickPhoto = {
                profilePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePhoto = {
                viewModel.removeProfilePhoto()
            },
            onNavigateToSettings = {
                showProfileDialog = false
                onNavigateToSettings()
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
                profilePhotoPath = profilePhotoPath,
                profilePhotoVersion = profilePhotoVersion,
                onAvatarClick = { showProfileDialog = true },
                onDirectPickPhoto = {
                    profilePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // ── Hero Tagline ──
        item {
            HeroSection(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
    profilePhotoPath: String?,
    profilePhotoVersion: Long,
    onAvatarClick: () -> Unit,
    onDirectPickPhoto: () -> Unit
) {
    val goldBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFFFFE57F),
                Color(0xFFD4AF37),
                Color(0xFFFFF6B8),
                Color(0xFFB8860B),
                Color(0xFFFFD700),
                Color(0xFFFBF0B9),
                Color(0xFFC59B27),
                Color(0xFFFFE57F)
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo & Brand Name (52dp Logo - Perfectly Symmetrical with Avatar)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            com.compressflow.app.presentation.components.CompressFlowLogo(size = 52.dp)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CompressFlow",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.6).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "SMART OFFLINE ENGINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Circular User Profile Avatar with Sparkling Luxury Gold Border (52dp - Exact Match with Logo)
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFFFFD700).copy(alpha = 0.45f),
                        spotColor = Color(0xFFD4AF37).copy(alpha = 0.65f)
                    )
                    .border(
                        width = 2.5.dp,
                        brush = goldBrush,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .clickable {
                        if (profilePhotoPath == null) {
                            onDirectPickPhoto()
                        } else {
                            onAvatarClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profilePhotoPath != null) {
                    key(profilePhotoVersion) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(profilePhotoPath))
                                .crossfade(true)
                                .build(),
                            contentDescription = "User Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User Avatar",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Glistening gold badge for photo upload indicator (direct to gallery)
            Box(
                modifier = Modifier
                    .size(19.dp)
                    .offset(x = 1.dp, y = 1.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFFE57F), Color(0xFFD4AF37))
                        ),
                        shape = CircleShape
                    )
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onDirectPickPhoto),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "Buka Galeri Foto",
                    tint = Color(0xFF1E1B00),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

// ── User Profile Photo Dialog ───────────────────────────────

@Composable
private fun UserProfileDialog(
    profilePhotoPath: String?,
    profilePhotoVersion: Long,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val goldBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFFFFE57F),
                Color(0xFFD4AF37),
                Color(0xFFFFF6B8),
                Color(0xFFB8860B),
                Color(0xFFFFD700),
                Color(0xFFFBF0B9),
                Color(0xFFC59B27),
                Color(0xFFFFE57F)
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Profil Pengguna",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large luxury gold circular preview (104dp)
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFFFD700).copy(alpha = 0.5f),
                            spotColor = Color(0xFFD4AF37).copy(alpha = 0.8f)
                        )
                        .border(
                            width = 3.5.dp,
                            brush = goldBrush,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoPath != null) {
                        key(profilePhotoVersion) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(File(profilePhotoPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto Profil Pengguna",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Default Avatar",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Text(
                    text = if (profilePhotoPath != null) 
                        "Foto profil aktif dan terpasang rapi dengan bingkai gold mewah." 
                    else 
                        "Belum ada foto profil. Unggah foto favorit Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "100% Privat & Tersimpan di perangkat (Zero Cloud)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPickPhoto,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (profilePhotoPath != null) "Buka Galeri (Ganti Foto)" else "Buka Galeri (Pilih Foto)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Pengambilan foto langsung terhubung ke Galeri perangkat (100% Offline & Aman).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (profilePhotoPath != null) {
                        OutlinedButton(
                            onClick = onRemovePhoto,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hapus Foto Profil", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Pengaturan Aplikasi")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ── Hero Section ────────────────────────────────────────────

@Composable
private fun HeroSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Make videos smaller.\nKeep them better.",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .offset(x = 210.dp, y = (-40).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .blur(40.dp)
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-30).dp, y = 110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    .blur(40.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Video",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
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

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "MP4, MOV, MKV up to 4K",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onBrowseVideos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Browse Videos",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
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
    val accentColor: Color
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
            CompressionPreset.WHATSAPP,
            Color(0xFF10B981)
        ),
        PresetItem(
            Icons.Outlined.Share,
            "Social Media",
            "1080p • 30fps Crisp",
            CompressionPreset.SOCIAL_MEDIA,
            Color(0xFF3B82F6)
        ),
        PresetItem(
            Icons.Outlined.Inventory2,
            "Storage Saver",
            "~70% space saved",
            CompressionPreset.STORAGE_SAVER,
            Color(0xFFF59E0B)
        ),
        PresetItem(
            Icons.Outlined.Email,
            "Email Friendly",
            "< 25 MB • Fast",
            CompressionPreset.EMAIL,
            Color(0xFF8B5CF6)
        )
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "One-tap profiles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.take(2).forEach { preset ->
                    PresetCard(
                        preset = preset,
                        modifier = Modifier.weight(1f),
                        onClick = { onPresetClick(preset.targetPreset) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(preset.accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = preset.label,
                        modifier = Modifier.size(22.dp),
                        tint = preset.accentColor
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
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
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.label,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
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
