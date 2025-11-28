package com.planned

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.planned.ui.theme.PlanEdTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context

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

        val db = AppDatabaseProvider.getDatabase(this)

        // DB dummy objects if developer enabled
        if (showDeveloper) {
            CoroutineScope(Dispatchers.IO).launch {
                runDummy(db)
            }
        }

        setContent {
            PlanEdTheme {
                AppNavigation()
            }
        }
    }
}