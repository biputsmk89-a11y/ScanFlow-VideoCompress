package com.compressflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.compressflow.app.data.preferences.AppSettings
import com.compressflow.app.data.preferences.SettingsRepository
import com.compressflow.app.presentation.navigation.CompressFlowNavHost
import com.compressflow.app.ui.theme.CompressFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val appSettings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
            val systemDark = isSystemInDarkTheme()

            val isDark = when (appSettings.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> if (appSettings.darkMode) true else systemDark
            }

            CompressFlowTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    CompressFlowNavHost()
                }
            }
        }
    }
}
