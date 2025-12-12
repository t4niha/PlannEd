package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/* SETTINGS */
object SettingsManager {
    var settings by mutableStateOf<AppSetting?>(null)
        private set

    // Load settings from DB
    suspend fun load(db: AppDatabase) {
        var s = db.settingsDao().getAll()

        // If null, insert defaults
        if (s == null) {
            s = AppSetting(
                id = 0,
                startWeekOnMonday = false,
                primaryColor = Converters.fromColor(Preset19),
                showDeveloper = true,
                breakDuration = 5,
                breakEvery = 30
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
    suspend fun setBreakDuration(db: AppDatabase, minutes: Int) {
        db.settingsDao().updateBreakDuration(minutes)
        settings = settings?.copy(breakDuration = minutes)
    }
    suspend fun setBreakEvery(db: AppDatabase, minutes: Int) {
        db.settingsDao().updateBreakEvery(minutes)
        settings = settings?.copy(breakEvery = minutes)
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

    // Update category
    suspend fun update(db: AppDatabase, category: Category) {
        db.categoryDao().update(category)
    }

    // Delete category and cascade null-setting
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun delete(db: AppDatabase, categoryId: Int) {
        onCategoryDeleted(db, categoryId)
        db.categoryDao().deleteById(categoryId)
    }
}

/* EVENT MANAGER */
object EventManager {
    // Insert new master event
    @RequiresApi(Build.VERSION_CODES.O)
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

        // Insert master event and get ID
        val insertedId = db.eventDao().insert(event).toInt()

        // Create a copy with ID
        val insertedEvent = event.copy(id = insertedId)

        // Generate occurrences
        val occurrences = generateEventOccurrences(insertedEvent)

        // Insert generated occurrences
        occurrences.forEach { occurrence ->
            db.eventDao().insertOccurrence(occurrence)
        }
    }

    // Get all master events
    suspend fun getAll(db: AppDatabase): List<MasterEvent> {
        return db.eventDao().getAllMasterEvents()
    }

    // Update master event
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun update(db: AppDatabase, event: MasterEvent) {
        db.eventDao().update(event)

        // Delete old occurrences
        val oldOccurrences = db.eventDao().getAllOccurrences()
            .filter { it.masterEventId == event.id }
        oldOccurrences.forEach { db.eventDao().deleteOccurrence(it.id) }

        // Regenerate occurrences
        val occurrences = generateEventOccurrences(event)
        occurrences.forEach { occurrence ->
            db.eventDao().insertOccurrence(occurrence)
        }
    }

    // Delete master event and cascade null-setting
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun delete(db: AppDatabase, eventId: Int) {
        onEventDeleted(db, eventId)
        db.eventDao().deleteMasterEvent(eventId)
    }
}

/* DEADLINE MANAGER */
object DeadlineManager {
    // Insert new deadline
    @RequiresApi(Build.VERSION_CODES.O)
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

        // Regenerate task intervals
        onTaskChanged(db)
    }

    // Get all deadlines
    suspend fun getAll(db: AppDatabase): List<Deadline> {
        return db.deadlineDao().getAll()
    }

    // Update deadline
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun update(db: AppDatabase, deadline: Deadline) {
        db.deadlineDao().update(deadline)

        // Regenerate task intervals
        onTaskChanged(db)
    }

    // Delete deadline and cascade null-setting
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun delete(db: AppDatabase, deadlineId: Int) {
        onDeadlineDeleted(db, deadlineId)
        db.deadlineDao().deleteById(deadlineId)
    }
}

/* TASK BUCKET MANAGER */
object TaskBucketManager {
    // Insert new master task bucket
    @RequiresApi(Build.VERSION_CODES.O)
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

        // Insert master bucket and get ID
        val insertedId = db.taskBucketDao().insert(bucket).toInt()

        // Create a copy with ID
        val insertedBucket = bucket.copy(id = insertedId)

        // Generate occurrences with merging
        val occurrences = generateTaskBucketOccurrences(insertedBucket, db)

        // Insert all generated occurrences
        occurrences.forEach { occurrence ->
            db.taskBucketDao().insertOccurrence(occurrence)
        }

        // Regenerate task intervals
        onTaskBucketDeleted(db)
    }

    // Get all master buckets
    suspend fun getAll(db: AppDatabase): List<MasterTaskBucket> {
        return db.taskBucketDao().getAllMasterBuckets()
    }

    // Update master bucket
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun update(db: AppDatabase, bucket: MasterTaskBucket) {
        db.taskBucketDao().update(bucket)

        // Delete old occurrences
        val oldOccurrences = db.taskBucketDao().getAllBucketOccurrences()
            .filter { it.masterBucketId == bucket.id }
        oldOccurrences.forEach { db.taskBucketDao().deleteOccurrence(it.id) }

        // Regenerate occurrences
        val occurrences = generateTaskBucketOccurrences(bucket, db)
        occurrences.forEach { occurrence ->
            db.taskBucketDao().insertOccurrence(occurrence)
        }

        // Regenerate task intervals with updated bucket times
        onTaskBucketDeleted(db)
    }

    // Delete master bucket
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun delete(db: AppDatabase, bucketId: Int) {
        db.taskBucketDao().deleteMasterBucket(bucketId)

        // Regenerate task intervals
        onTaskBucketDeleted(db)
    }
}

/* TASK MANAGER */
object TaskManager {
    // Insert new master task
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun insert(
        db: AppDatabase,
        title: String,
        notes: String?,
        priority: Int,
        breakable: Boolean,
        startDate: LocalDate?,
        startTime: LocalTime?,
        predictedDuration: Int,
        categoryId: Int?,
        eventId: Int?,
        deadlineId: Int?,
        dependencyTaskId: Int?
    ) {
        val task = MasterTask(
            title = title,
            notes = notes,
            priority = priority,
            breakable = breakable,
            noIntervals = 0,
            startDate = startDate,
            startTime = startTime,
            predictedDuration = predictedDuration,
            categoryId = categoryId,
            eventId = eventId,
            deadlineId = deadlineId,
            dependencyTaskId = dependencyTaskId
        )
        db.taskDao().insert(task)

        // Regenerate task intervals
        onTaskChanged(db)
    }

    // Get all master tasks
    suspend fun getAll(db: AppDatabase): List<MasterTask> {
        return db.taskDao().getAllMasterTasks()
    }

    // Update master task
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun update(db: AppDatabase, task: MasterTask) {
        db.taskDao().update(task)

        // Regenerate task intervals
        onTaskChanged(db)
    }

    // Delete master task
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun delete(db: AppDatabase, taskId: Int) {
        // Handle dependency
        onTaskDeleted(db, taskId)

        db.taskDao().deleteMasterTask(taskId)
    }
}

/* REMINDER MANAGER */
object ReminderManager {
    // Insert new master reminder
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun insert(
        db: AppDatabase,
        title: String,
        notes: String?,
        color: Color,
        startDate: LocalDate,
        endDate: LocalDate?,
        time: LocalTime?,
        allDay: Boolean,
        recurFreq: RecurrenceFrequency,
        recurRule: RecurrenceRule,
        categoryId: Int?
    ) {
        val reminder = MasterReminder(
            title = title,
            notes = notes,
            color = Converters.fromColor(color),
            startDate = startDate,
            endDate = endDate,
            time = time,
            allDay = allDay,
            recurFreq = recurFreq,
            recurRule = recurRule,
            categoryId = categoryId
        )

        // Insert master reminder and get ID
        val insertedId = db.reminderDao().insert(reminder).toInt()

        // Create a copy with ID
        val insertedReminder = reminder.copy(id = insertedId)

        // Generate occurrences
        val occurrences = generateReminderOccurrences(insertedReminder)

        // Insert all generated occurrences
        occurrences.forEach { occurrence ->
            db.reminderDao().insertOccurrence(occurrence)
        }
    }

    // Get all master reminders
    suspend fun getAll(db: AppDatabase): List<MasterReminder> {
        return db.reminderDao().getAllMasterReminders()
    }

    // Update master reminder
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun update(db: AppDatabase, reminder: MasterReminder) {
        db.reminderDao().update(reminder)

        // Delete old occurrences
        val oldOccurrences = db.reminderDao().getAllOccurrences()
            .filter { it.masterReminderId == reminder.id }
        oldOccurrences.forEach { db.reminderDao().deleteOccurrence(it.id) }

        // Regenerate occurrences
        val occurrences = generateReminderOccurrences(reminder)
        occurrences.forEach { occurrence ->
            db.reminderDao().insertOccurrence(occurrence)
        }
    }

    // Delete master reminder
    suspend fun delete(db: AppDatabase, reminderId: Int) {
        db.reminderDao().deleteMasterReminder(reminderId)
    }
}