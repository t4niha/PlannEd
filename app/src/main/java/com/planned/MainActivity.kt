package com.planned

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.planned.ui.theme.PlanEdTheme
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

object AppDatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    // Global database instance
    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "planned-db"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val db = AppDatabaseProvider.getDatabase(this)
            PlanEdTheme {
                SetupDefaultSettings(db)
                AppNavigation()
            }
        }
    }
}

// Default settings
@Composable
fun SetupDefaultSettings(db: AppDatabase) {
    LaunchedEffect(Unit) {
        val currentSettings = db.settingsDao().getSettings()
        if (currentSettings == null) {
            val defaultSetting = AppSetting(
                id = 0,
                startWeekOnMonday = false,
                primaryColor = Converters.fromColor(Preset19),
                showDeveloper = true
            )
            db.settingsDao().insertOrUpdate(defaultSetting)
        }
    }
}