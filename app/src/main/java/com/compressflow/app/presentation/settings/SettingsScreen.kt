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
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }

    fun openPrivacyUrl() {
        val url = "https://github.com/compressflow/privacy-policy"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "CompressFlow processes and compresses your videos entirely on your local hardware. None of your media files, metadata, or personal information are ever uploaded to any cloud server or third party.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Permissions:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }

        // Output section
        item {
            SettingsSectionHeader("Output")
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.Folder,
                title = "Save Location",
                subtitle = "Default • Movies/CompressFlow"
            )
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.TextFields,
                title = "Filename Pattern",
                subtitle = "{name}_compressed"
            )
        }

        // Compression section
        item {
            SettingsSectionHeader("Compression")
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.Speed,
                title = "Default Quality",
                subtitle = "Balanced"
            )
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.VideoSettings,
                title = "Default Codec",
                subtitle = "Auto (best available)"
            )
        }
        item {
            var keepAudio by remember { mutableStateOf(true) }
            SettingsToggleItem(
                icon = Icons.Outlined.MusicNote,
                title = "Keep Audio",
                subtitle = "Preserve audio track by default",
                checked = keepAudio,
                onCheckedChange = { keepAudio = it }
            )
        }

        // App section
        item {
            SettingsSectionHeader("App")
        }
        item {
            var darkMode by remember { mutableStateOf(false) }
            SettingsToggleItem(
                icon = Icons.Outlined.DarkMode,
                title = "Dark Mode",
                subtitle = "Follow system theme",
                checked = darkMode,
                onCheckedChange = { darkMode = it }
            )
        }
        item {
            var notifications by remember { mutableStateOf(true) }
            SettingsToggleItem(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                subtitle = "Show compression progress",
                checked = notifications,
                onCheckedChange = { notifications = it }
            )
        }

        // About section
        item {
            SettingsSectionHeader("About")
        }
        item {
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Version",
                subtitle = "1.0.0"
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
                color = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurface
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
