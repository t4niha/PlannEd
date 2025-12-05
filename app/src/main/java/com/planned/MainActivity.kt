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

        // Load DB
        val db = AppDatabaseProvider.getDatabase(this)
        setContent {

            // Load settings and regenerate occurrences
            LaunchedEffect(Unit) {
                SettingsManager.load(db)
                regenerateAllOccurrences(db)
            }
            // Home page
            PlanEdTheme {
                AppNavigation(db)
            }
        }
    }
}

/**
 TODO: Task form dropdown cascade logic issue, isEditMode needed?
 TODO: Cancel (dialogue) buttons do same as Save
 TODO: Task form update needed after Save for dependency option
 TODO: UI Pages + isException logic
 TODO: Calendar to object pages
 TODO: Scheduling breakable tasks only partially when insufficient bucket space
 TODO: Warning for insufficient bucket space
 TODO: What to do when impossible to meet deadline given current schedule order?
**/