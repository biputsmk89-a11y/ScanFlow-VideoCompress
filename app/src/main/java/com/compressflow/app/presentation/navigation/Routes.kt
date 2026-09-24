package com.compressflow.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation route definitions for CompressFlow.
 */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val VIDEO_DETAIL = "video_detail/{videoUri}"
    const val COMPRESSION = "compression"
    const val RESULT = "result"
    const val BATCH_SELECT = "batch_select"
    const val BATCH_PROGRESS = "batch_progress"
    const val BATCH_COMPLETE = "batch_complete"
    const val ADVANCED_SETTINGS = "advanced_settings"
    const val STORAGE_ANALYZER = "storage_analyzer"
    const val CHOOSE_TARGET_SIZE = "choose_target_size"
    const val QUALITY_COMPARE = "quality_compare"
    const val TRIM_VIDEO = "trim_video?videoUri={videoUri}"

    fun videoDetail(videoUri: String): String =
        "video_detail/${android.net.Uri.encode(videoUri)}"

    fun trimVideo(videoUri: String? = null): String =
        if (!videoUri.isNullOrBlank()) {
            "trim_video?videoUri=${android.net.Uri.encode(videoUri)}"
        } else {
            "trim_video?videoUri="
        }
}

/**
 * Bottom navigation items.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Routes.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = Routes.HISTORY,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    BottomNavItem(
        route = Routes.TOOLS,
        label = "Tools",
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build
    ),
    BottomNavItem(
        route = Routes.SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
