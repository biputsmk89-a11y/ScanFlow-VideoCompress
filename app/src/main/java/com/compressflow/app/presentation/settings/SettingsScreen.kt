package com.compressflow.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showCodecDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    fun openPrivacyUrl() {
        val url = "https://github.com/biputsmk89-a11y/ScanFlow-VideoCompress#privacy"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Theme Selection Dialog (Light Mode, Dark Mode, System) ──
    if (showThemeDialog) {
        val themes = listOf(
            Triple("LIGHT", "Light Mode", "Mode Terang dengan kontras jernih"),
            Triple("DARK", "Dark Mode", "Mode Gelap hemat baterai"),
            Triple("SYSTEM", "Follow System", "Mengikuti pengaturan tema HP")
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    themes.forEach { (mode, label, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // ── Save Location Dialog ──
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Save Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Standard Media Directory:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "/storage/emulated/0/${settings.saveLocation}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "All compressed videos are stored in public storage and automatically indexed into your phone's Gallery / Google Photos app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // ── Filename Pattern Dialog ──
    if (showPatternDialog) {
        val patterns = listOf(
            Pair("{name}_compressed", "video_compressed.mp4 (Default)"),
            Pair("VID_{date}_{name}", "VID_20260924_video.mp4 (Timestamp)"),
            Pair("{name}_small", "video_small.mp4 (Short label)"),
            Pair("{name}_cf", "video_cf.mp4 (CompressFlow tag)")
        )
        AlertDialog(
            onDismissRequest = { showPatternDialog = false },
            title = { Text("Filename Pattern") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    patterns.forEach { (pattern, example) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFilenamePattern(pattern)
                                    showPatternDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.filenamePattern == pattern,
                                onClick = {
                                    viewModel.setFilenamePattern(pattern)
                                    showPatternDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = pattern, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(text = example, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPatternDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Default Quality Dialog ──
    if (showQualityDialog) {
        val qualities = listOf(
            Pair("Balanced", "Best balance between size reduction and visual clarity"),
            Pair("High Quality", "1080p crisp preservation for social media"),
            Pair("Maximum Space Saving", "Aggressive compression (~70% - 90% saved)")
        )
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Default Quality Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    qualities.forEach { (q, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.defaultQuality == q,
                                onClick = {
                                    viewModel.setDefaultQuality(q)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = q, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Default Codec Dialog ──
    if (showCodecDialog) {
        val codecs = listOf(
            Pair("Auto (best available)", "Uses hardware H.265 if available, fallback to H.264"),
            Pair("H.265 / HEVC (Best compression)", "40% smaller file size at same visual quality"),
            Pair("H.264 (Maximum compatibility)", "Plays on virtually every legacy device"),
            Pair("AV1 (Next-gen)", "Next-generation royalty-free open codec")
        )
        AlertDialog(
            onDismissRequest = { showCodecDialog = false },
            title = { Text("Default Video Codec") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    codecs.forEach { (c, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultCodec(c)
                                    showCodecDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.defaultCodec == c,
                                onClick = {
                                    viewModel.setDefaultCodec(c)
                                    showCodecDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = c, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCodecDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Privacy Policy Dialog ──
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(text = "Privacy Policy", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "100% On-Device & Offline:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CompressFlow processes and compresses your videos entirely on your local hardware. None of your media files, metadata, or personal information are ever uploaded to any cloud server or third party.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Permissions:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Media access permissions are only used to let you select source videos and save the compressed outputs to your gallery.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPrivacyDialog = false
                        openPrivacyUrl()
                    }
                ) {
                    Text("Open Web Policy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // ── About App Dialog ──
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                com.compressflow.app.presentation.components.CompressFlowLogo(size = 48.dp)
            },
            title = { Text("CompressFlow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version: ${com.compressflow.app.BuildConfig.VERSION_NAME} (Production Release)", fontWeight = FontWeight.Bold)
                    Text("Engine: AndroidX Media3 Transformer")
                    Text("Hardware Acceleration: MediaCodec VBR")
                    Text("Database: Room Persistence + DataStore")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Crafted for high efficiency, privacy, and speed without cloud dependencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }

        // ── Output Section ──
        item {
            SettingsSectionHeader("Output")
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.Folder,
                title = "Save Location",
                subtitle = "Default • ${settings.saveLocation}",
                onClick = { showLocationDialog = true }
            )
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.TextFields,
                title = "Filename Pattern",
                subtitle = settings.filenamePattern,
                onClick = { showPatternDialog = true }
            )
        }

        // ── Compression Section ──
        item {
            SettingsSectionHeader("Compression")
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.Speed,
                title = "Default Quality",
                subtitle = settings.defaultQuality,
                onClick = { showQualityDialog = true }
            )
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.VideoSettings,
                title = "Default Codec",
                subtitle = settings.defaultCodec,
                onClick = { showCodecDialog = true }
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Outlined.MusicNote,
                title = "Keep Audio",
                subtitle = "Preserve audio track by default",
                checked = settings.keepAudio,
                onCheckedChange = { viewModel.toggleKeepAudio(it) }
            )
        }

        // ── App & Theme Section ──
        item {
            SettingsSectionHeader("App")
        }
        item {
            val themeSubtitle = when (settings.themeMode) {
                "LIGHT" -> "Light Mode (Active)"
                "DARK" -> "Dark Mode (Active)"
                else -> "Follow system theme"
            }
            val themeIcon = when (settings.themeMode) {
                "LIGHT" -> Icons.Outlined.LightMode
                "DARK" -> Icons.Outlined.DarkMode
                else -> Icons.Outlined.BrightnessAuto
            }
            SettingsItem(
                icon = themeIcon,
                title = "Theme / Dark Mode",
                subtitle = themeSubtitle,
                onClick = { showThemeDialog = true }
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                subtitle = "Show compression progress",
                checked = settings.notifications,
                onCheckedChange = { viewModel.toggleNotifications(it) }
            )
        }

        // ── About Section ──
        item {
            SettingsSectionHeader("About")
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Version",
                subtitle = "${com.compressflow.app.BuildConfig.VERSION_NAME} (Build ${com.compressflow.app.BuildConfig.VERSION_CODE}) • Hardware Engine",
                onClick = { showAboutDialog = true }
            )
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "100% offline • No data collected",
                onClick = { showPrivacyDialog = true }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
