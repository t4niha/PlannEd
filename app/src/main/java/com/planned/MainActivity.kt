package com.planned

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.planned.ui.theme.PlanEdTheme
import android.content.Context
import android.content.pm.PackageManager
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
            ).fallbackToDestructiveMigration(true).build()
            INSTANCE = instance
            instance
        }
    }
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    .launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request exact alarm permission on API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName"))
                )
            }
        }

        // Load DB
        val db = AppDatabaseProvider.getDatabase(this)
        val appContext = applicationContext
        setContent {

            // Load settings and regenerate occurrences
            LaunchedEffect(Unit) {
                SettingsManager.load(db)
                trimAndExtendOccurrences(appContext, db)
                NotificationScheduler.rescheduleAll(appContext, db)
            }
            // Home page
            PlanEdTheme {
                AppNavigation(db)
            }
        }
    }
}

/**
[ ] Pretrained model parts
 **/

/*  */
//<editor-fold desc="">
//</editor-fold>