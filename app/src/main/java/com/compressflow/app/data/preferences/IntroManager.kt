package com.compressflow.app.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the first-launch intro / splash state.
 * Uses fast synchronous SharedPreferences to prevent UI flicker or blank frames on cold launch.
 */
class IntroManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun hasCompletedIntro(): Boolean {
        return prefs.getBoolean(KEY_INTRO_COMPLETED, false)
    }

    fun setIntroCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_INTRO_COMPLETED, completed).apply()
    }

    fun resetIntro() {
        prefs.edit().remove(KEY_INTRO_COMPLETED).apply()
    }

    companion object {
        private const val PREFS_NAME = "compressflow_intro_prefs"
        private const val KEY_INTRO_COMPLETED = "has_completed_first_launch_intro"
    }
}
