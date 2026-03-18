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
TODO: [-] Deadlines/Events info pages -> Related Tasks
TODO: [-] Scheduler schedules tasks before right now??????
TODO: [-] Fix UI pages (category, event, deadline, reminder, task bucket)
TODO: [-] Calendar to UI pages navigation
TODO: [ ] On creation don't allow duplicate titles for events, categories
TODO: [ ] refresh schedule button?
TODO: [-] Confirmation dialogs for destructive operations (this/all)
TODO: [ ] Warning for insufficient bucket space
TODO: [ ] Warning when tasks scheduled after deadline / impossible to meet deadline

TODO: [/] Kanban board?
TODO: [-] POMODORO have the option to control timer spread (not just an hour!)
TODO: [ ] DB Sample stuff

TODO: [ ] notifications?
TODO: [ ] Status indicators and overtime warnings for task elements on calendar?

TODO: [ ] Task info back to calendar doesn't scroll to that task, only to today
TODO: [-] Task update form dropdown loop glitch
TODO: [-] Cancel (dialogue) buttons do same as Save?
TODO: [-] Task form update needed after Save for dependency option
 **/