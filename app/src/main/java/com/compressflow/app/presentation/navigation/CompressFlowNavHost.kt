package com.compressflow.app.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.compressflow.app.presentation.batch.BatchSelectScreen
import com.compressflow.app.presentation.compression.AdvancedSettingsScreen
import com.compressflow.app.presentation.compression.ChooseTargetSizeScreen
import com.compressflow.app.presentation.compression.CompressionScreen
import com.compressflow.app.presentation.history.HistoryScreen
import com.compressflow.app.presentation.home.HomeScreen
import com.compressflow.app.presentation.result.QualityComparisonScreen
import com.compressflow.app.presentation.result.ResultScreen
import com.compressflow.app.presentation.settings.SettingsScreen
import com.compressflow.app.presentation.tools.StorageAnalyzerScreen
import com.compressflow.app.presentation.tools.ToolsScreen
import com.compressflow.app.presentation.video_detail.VideoDetailScreen

@Composable
fun CompressFlowNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar on top-level destinations
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToVideoDetail = { uri ->
                        navController.navigate(Routes.videoDetail(uri))
                    },
                    onNavigateToBatchSelect = {
                        navController.navigate(Routes.BATCH_SELECT)
                    },
                    onNavigateToStorageAnalyzer = {
                        navController.navigate(Routes.STORAGE_ANALYZER)
                    }
                )
            }

            composable(
                route = Routes.VIDEO_DETAIL,
                arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoUri = backStackEntry.arguments?.getString("videoUri") ?: ""
                val decodedUri = android.net.Uri.decode(videoUri)
                VideoDetailScreen(
                    videoUri = decodedUri,
                    onNavigateBack = { navController.popBackStack() },
                    onContinueToCompress = { navController.navigate(Routes.COMPRESSION) },
                    onNavigateToAdvanced = { navController.navigate(Routes.ADVANCED_SETTINGS) },
                    onNavigateToTargetSize = { navController.navigate(Routes.CHOOSE_TARGET_SIZE) }
                )
            }

            composable(Routes.CHOOSE_TARGET_SIZE) {
                ChooseTargetSizeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onContinueToCompress = { navController.navigate(Routes.COMPRESSION) }
                )
            }

            composable(Routes.COMPRESSION) {
                CompressionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCompressionFinished = {
                        navController.navigate(Routes.RESULT) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                )
            }

            composable(Routes.RESULT) {
                ResultScreen(
                    onNavigateHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onCompressAnother = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateToQualityCompare = {
                        navController.navigate(Routes.QUALITY_COMPARE)
                    }
                )
            }

            composable(Routes.QUALITY_COMPARE) {
                QualityComparisonScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.BATCH_SELECT) {
                BatchSelectScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onStartBatch = { uris ->
                        if (uris.isNotEmpty()) {
                            navController.navigate(Routes.videoDetail(uris.first().toString()))
                        }
                    }
                )
            }

            composable(Routes.STORAGE_ANALYZER) {
                StorageAnalyzerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onBatchCompress = { navController.navigate(Routes.BATCH_SELECT) }
                )
            }

            composable(Routes.ADVANCED_SETTINGS) {
                AdvancedSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onApplyAndCompress = { navController.navigate(Routes.COMPRESSION) }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen()
            }

            composable(Routes.TOOLS) {
                ToolsScreen(
                    onNavigateToStorageAnalyzer = {
                        navController.navigate(Routes.STORAGE_ANALYZER)
                    },
                    onNavigateToBatchSelect = {
                        navController.navigate(Routes.BATCH_SELECT)
                    },
                    onNavigateToQualityCompare = {
                        navController.navigate(Routes.QUALITY_COMPARE)
                    },
                    onNavigateToVideoDetail = { uri ->
                        navController.navigate(Routes.videoDetail(uri))
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
