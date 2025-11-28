package com.planned

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/* SETTINGS */
object SettingsManager {
    var settings by mutableStateOf<AppSetting?>(null)
        private set

    // Load settings from DB
    suspend fun load(db: AppDatabase) {
        var s = db.settingsDao().getAll()

        // If null, create defaults and insert
        if (s == null) {
            s = AppSetting(
                id = 0,
                startWeekOnMonday = false,
                primaryColor = Converters.fromColor(Preset19),
                showDeveloper = true
            )
            db.settingsDao().insert(s)
        }
        settings = s
    }

    // Update fields
    suspend fun setPrimaryColor(db: AppDatabase, color: Color) {
        val hex = Converters.fromColor(color)
        db.settingsDao().updatePrimaryColor(hex)
        settings = settings?.copy(primaryColor = hex)
    }
    suspend fun setStartWeek(db: AppDatabase, monday: Boolean) {
        db.settingsDao().updateStartWeekOnMonday(monday)
        settings = settings?.copy(startWeekOnMonday = monday)
    }
    suspend fun setDeveloperMode(db: AppDatabase, enabled: Boolean) {
        db.settingsDao().updateShowDeveloper(enabled)
        settings = settings?.copy(showDeveloper = enabled)
    }
}

/*  */