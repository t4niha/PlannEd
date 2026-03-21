package com.planned

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.planned.ui.theme.PlanEdTheme
import android.content.Context
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
            ).fallbackToDestructiveMigration(false).build()
            INSTANCE = instance
            instance
        }
    }
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load DB
        val db = AppDatabaseProvider.getDatabase(this)
        setContent {

            // Load settings and regenerate occurrences
            LaunchedEffect(Unit) {
                SettingsManager.load(db)
                trimAndExtendOccurrences(db)
            }
            // Home page
            PlanEdTheme {
                AppNavigation(db)
            }
        }
    }
}

/**
[?] On creation don't allow duplicate titles for events, categories?
[?] Warning when tasks scheduled after deadline / impossible to meet deadline?
[/] Kanban board?
[?] Notifications
[?] Status indicators and overtime warnings for task elements on calendar?
 **/

/*  */
//<editor-fold desc="">
//</editor-fold>