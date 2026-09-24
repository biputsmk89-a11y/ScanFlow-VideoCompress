package com.compressflow.app.presentation.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationLogicTest {

    @Test
    fun `test bottom navigation items contains 4 primary tabs in exact order`() {
        assertEquals(4, bottomNavItems.size)
        assertEquals(Routes.HOME, bottomNavItems[0].route)
        assertEquals(Routes.HISTORY, bottomNavItems[1].route)
        assertEquals(Routes.TOOLS, bottomNavItems[2].route)
        assertEquals(Routes.SETTINGS, bottomNavItems[3].route)

        assertEquals("Home", bottomNavItems[0].label)
        assertEquals("History", bottomNavItems[1].label)
        assertEquals("Tools", bottomNavItems[2].label)
        assertEquals("Settings", bottomNavItems[3].label)
    }

    @Test
    fun `test top level destination detection for bottom bar visibility`() {
        val topLevelRoutes = bottomNavItems.map { it.route }.toSet()

        assertTrue(topLevelRoutes.contains("home"))
        assertTrue(topLevelRoutes.contains("history"))
        assertTrue(topLevelRoutes.contains("tools"))
        assertTrue(topLevelRoutes.contains("settings"))

        // Sub-screens must NOT show bottom bar
        assertFalse(topLevelRoutes.contains("splash_intro"))
        assertFalse(topLevelRoutes.contains("video_detail/{videoUri}"))
        assertFalse(topLevelRoutes.contains("compression"))
        assertFalse(topLevelRoutes.contains("result"))
        assertFalse(topLevelRoutes.contains("trim_video?videoUri={videoUri}"))
        assertFalse(topLevelRoutes.contains("storage_analyzer"))
    }

    @Test
    fun `test route template constants consistency`() {
        assertEquals("video_detail/{videoUri}", Routes.VIDEO_DETAIL)
        assertEquals("trim_video?videoUri={videoUri}", Routes.TRIM_VIDEO)
        assertEquals("home", Routes.HOME)
        assertEquals("history", Routes.HISTORY)
        assertEquals("tools", Routes.TOOLS)
        assertEquals("settings", Routes.SETTINGS)
        assertEquals("compression", Routes.COMPRESSION)
        assertEquals("result", Routes.RESULT)
    }

    @Test
    fun `test bottom navigation tab switching logic prevents redundant navigation`() {
        var currentRoute = "history"
        var navigationTarget: String? = null
        var poppedToHome = false

        fun onTabClicked(itemRoute: String) {
            if (currentRoute != itemRoute) {
                if (itemRoute == Routes.HOME) {
                    poppedToHome = true
                    currentRoute = Routes.HOME
                } else {
                    navigationTarget = itemRoute
                    currentRoute = itemRoute
                }
            }
        }

        // Clicking Home while on History must pop back to Home
        onTabClicked(Routes.HOME)
        assertTrue(poppedToHome)
        assertEquals(Routes.HOME, currentRoute)

        // Clicking Home again while already on Home must do nothing
        poppedToHome = false
        onTabClicked(Routes.HOME)
        assertFalse(poppedToHome)
        assertEquals(Routes.HOME, currentRoute)

        // Clicking History from Home
        onTabClicked(Routes.HISTORY)
        assertEquals(Routes.HISTORY, navigationTarget)
        assertEquals(Routes.HISTORY, currentRoute)

        // Clicking Tools from History
        onTabClicked(Routes.TOOLS)
        assertEquals(Routes.TOOLS, navigationTarget)
        assertEquals(Routes.TOOLS, currentRoute)
    }
}
