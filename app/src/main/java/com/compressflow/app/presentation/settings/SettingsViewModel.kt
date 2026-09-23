package com.compressflow.app.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compressflow.app.data.preferences.AppSettings
import com.compressflow.app.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun toggleKeepAudio(enabled: Boolean) {
        viewModelScope.launch {
            repository.setKeepAudio(enabled)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotifications(enabled)
        }
    }

    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            repository.setDefaultQuality(quality)
        }
    }

    fun setDefaultCodec(codec: String) {
        viewModelScope.launch {
            repository.setDefaultCodec(codec)
        }
    }
}
