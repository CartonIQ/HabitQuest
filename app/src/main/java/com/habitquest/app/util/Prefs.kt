package com.habitquest.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object Prefs {
    private const val FILE = "habitquest_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_REMINDERS = "reminders_enabled"
    private const val KEY_FREEZE_DAY = "freeze_prompt_day"
    private const val KEY_ASKED_NOTIF = "asked_notification_permission"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_HAPTICS = "haptics_enabled"
    private const val KEY_AD_FREE = "ad_free_mode"
    private const val KEY_LAST_SEEN_LEVEL = "last_seen_level"

    private fun sp(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun themeMode(context: Context): Int =
        sp(context).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setThemeMode(context: Context, mode: Int) {
        sp(context).edit().putInt(KEY_THEME, mode).apply()
    }

    fun remindersEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_REMINDERS, true)

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_REMINDERS, enabled).apply()
    }

    fun soundEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun hapticsEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_HAPTICS, true)

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_HAPTICS, enabled).apply()
    }

    fun isAdFreeMode(context: Context): Boolean =
        sp(context).getBoolean(KEY_AD_FREE, false)

    fun setAdFreeMode(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_AD_FREE, enabled).apply()
    }

    fun lastFreezePromptDay(context: Context): Long =
        sp(context).getLong(KEY_FREEZE_DAY, -1L)

    fun setLastFreezePromptDay(context: Context, day: Long) {
        sp(context).edit().putLong(KEY_FREEZE_DAY, day).apply()
    }

    fun askedNotificationPermission(context: Context): Boolean =
        sp(context).getBoolean(KEY_ASKED_NOTIF, false)

    fun setAskedNotificationPermission(context: Context) {
        sp(context).edit().putBoolean(KEY_ASKED_NOTIF, true).apply()
    }

    fun lastSeenLevel(context: Context): Int =
        sp(context).getInt(KEY_LAST_SEEN_LEVEL, 1)

    fun setLastSeenLevel(context: Context, level: Int) {
        sp(context).edit().putInt(KEY_LAST_SEEN_LEVEL, level).apply()
    }
}

// Alias for convenience
fun Prefs.setSeenLevel(context: android.content.Context, level: Int) = setLastSeenLevel(context, level)
