package com.compressflow.app.presentation.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

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
    onNavigateToVideoDetail: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var comingSoonTool by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    if (comingSoonTool != null) {
        AlertDialog(
            onDismissRequest = { comingSoonTool = null },
            title = { Text("$comingSoonTool") },
            text = {
                Text(
                    "$comingSoonTool is currently under development for the next CompressFlow release. Enjoy our high-speed compression and storage analyzer features in the meantime!"
                )
            },
            confirmButton = {
                TextButton(onClick = { comingSoonTool = null }) {
                    Text("OK")
                }
            }
        )
    }

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
            description = "Cut start and end points",
            badge = "Coming Soon",
            onClick = { comingSoonTool = "Trim Video" }
        ),
        ToolGridItem(
            icon = Icons.Outlined.SwapHoriz,
            label = "Video Converter",
            description = "Change format & codec",
            badge = "Coming Soon",
            onClick = { comingSoonTool = "Video Converter" }
        ),
        ToolGridItem(
            icon = Icons.Outlined.MusicNote,
            label = "Extract Audio",
            description = "Save audio track as M4A",
            badge = "Coming Soon",
            onClick = { comingSoonTool = "Extract Audio" }
        ),
        ToolGridItem(
            icon = Icons.AutoMirrored.Outlined.VolumeOff,
            label = "Remove Audio",
            description = "Silent video output",
            badge = "Coming Soon",
            onClick = { comingSoonTool = "Remove Audio" }
        ),
        ToolGridItem(
            icon = Icons.Outlined.DeleteSweep,
            label = "Remove Metadata",
            description = "Strip private GPS & device tags",
            badge = "Coming Soon",
            onClick = { comingSoonTool = "Remove Metadata" }
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
