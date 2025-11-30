package com.planned

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime

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

/* CATEGORY MANAGER */
object CategoryManager {
    // Insert new category
    suspend fun insert(
        db: AppDatabase,
        title: String,
        notes: String?,
        color: Color
    ) {
        val category = Category(
            title = title,
            notes = notes,
            color = Converters.fromColor(color)
        )
        db.categoryDao().insert(category)
    }

    // Get all categories
    suspend fun getAll(db: AppDatabase): List<Category> {
        return db.categoryDao().getAll()
    }
}

/* EVENT MANAGER */
object EventManager {
    // Insert new master event
    suspend fun insert(
        db: AppDatabase,
        title: String,
        notes: String?,
        color: Color?,
        startDate: LocalDate,
        endDate: LocalDate?,
        startTime: LocalTime,
        endTime: LocalTime,
        recurFreq: RecurrenceFrequency,
        recurRule: RecurrenceRule,
        categoryId: Int?
    ) {
        val event = MasterEvent(
            title = title,
            notes = notes,
            color = color?.let { Converters.fromColor(it) },
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            recurFreq = recurFreq,
            recurRule = recurRule,
            categoryId = categoryId
        )
        db.eventDao().insert(event)

        // TODO: Generate occurrences using DB_Generator
    }

    // Get all master events
    suspend fun getAll(db: AppDatabase): List<MasterEvent> {
        return db.eventDao().getAllMasterEvents()
    }
}

/* DEADLINE MANAGER */
object DeadlineManager {
    // Insert new deadline
    suspend fun insert(
        db: AppDatabase,
        title: String,
        notes: String?,
        date: LocalDate,
        time: LocalTime,
        categoryId: Int?,
        eventId: Int?
    ) {
        val deadline = Deadline(
            title = title,
            notes = notes,
            date = date,
            time = time,
            categoryId = categoryId,
            eventId = eventId
        )
        db.deadlineDao().insert(deadline)
    }

    // Get all deadlines
    suspend fun getAll(db: AppDatabase): List<Deadline> {
        return db.deadlineDao().getAll()
    }
}

/* TASK BUCKET MANAGER */
object TaskBucketManager {
    // Insert new master task bucket
    suspend fun insert(
        db: AppDatabase,
        startDate: LocalDate,
        endDate: LocalDate?,
        startTime: LocalTime,
        endTime: LocalTime,
        recurFreq: RecurrenceFrequency,
        recurRule: RecurrenceRule
    ) {
        val bucket = MasterTaskBucket(
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            recurFreq = recurFreq,
            recurRule = recurRule
        )
        db.taskBucketDao().insert(bucket)

        // TODO: Generate occurrences using DB_Generator
    }

    // Get all master buckets
    suspend fun getAll(db: AppDatabase): List<MasterTaskBucket> {
        return db.taskBucketDao().getAllMasterBuckets()
    }
}

/* TASK MANAGER */
object TaskManager {
    // Insert new master task
    suspend fun insert(
        db: AppDatabase,
        title: String,
        notes: String?,
        priority: Int,
        breakable: Boolean,
        startDate: LocalDate?,
        startTime: LocalTime?,
        predictedDuration: Int, // in minutes
        categoryId: Int?,
        eventId: Int?,
        deadlineId: Int?
    ) {
        val task = MasterTask(
            title = title,
            notes = notes,
            priority = priority,
            breakable = breakable,
            noIntervals = if (breakable) 1 else 1, // Default 1, can be updated later
            startDate = startDate,
            startTime = startTime,
            predictedDuration = predictedDuration,
            categoryId = categoryId,
            eventId = eventId,
            deadlineId = deadlineId
        )
        db.taskDao().insert(task)

        // TODO: Generate intervals using DB_Generator
    }

    // Get all master tasks
    suspend fun getAll(db: AppDatabase): List<MasterTask> {
        return db.taskDao().getAllMasterTasks()
    }
}

/* REMINDER MANAGER */
object ReminderManager {
    // Insert new reminder
    suspend fun insert(
        db: AppDatabase,
        title: String,
        notes: String?,
        color: Color,
        date: LocalDate,
        time: LocalTime?,
        allDay: Boolean
    ) {
        val reminder = Reminder(
            title = title,
            notes = notes,
            color = Converters.fromColor(color),
            date = date,
            time = time,
            allDay = allDay
        )
        db.reminderDao().insert(reminder)
    }

    // Get all reminders
    suspend fun getAll(db: AppDatabase): List<Reminder> {
        return db.reminderDao().getAll()
    }
}