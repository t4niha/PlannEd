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

        // If null, create defaults and insert
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

        // Generate occurrences automatically
        val occurrences = generateEventOccurrences(insertedEvent)

        // Insert all generated occurrences
        occurrences.forEach { occurrence ->
            db.eventDao().insertOccurrence(occurrence)
        }
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

        // Generate occurrences with merging logic automatically
        val occurrences = generateTaskBucketOccurrences(insertedBucket, db)

        // Insert all generated occurrences
        occurrences.forEach { occurrence ->
            db.taskBucketDao().insertOccurrence(occurrence)
        }
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

        // TODO: Generate intervals using DB_Generator (after heuristic algorithm)
    }

    // Get all master tasks
    suspend fun getAll(db: AppDatabase): List<MasterTask> {
        return db.taskDao().getAllMasterTasks()
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

        // Generate occurrences automatically
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
}

/* OVERLAP DETECTION */
//<editor-fold desc="Overlap">

data class OverlapInfo(
    val hasOverlap: Boolean,
    val conflictType: String = "",
    val conflictDate: LocalDate? = null,
    val conflictStartTime: LocalTime? = null,
    val conflictEndTime: LocalTime? = null
)

/* Check if two time ranges overlap */
@RequiresApi(Build.VERSION_CODES.O)
fun doTimeRangesOverlap(
    start1: LocalTime, end1: LocalTime,
    start2: LocalTime, end2: LocalTime
): Boolean {
    return !start1.isAfter(end2) && !end1.isBefore(start2)
}

/* Check if a new Event would overlap with existing Task Bucket occurrences */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun checkEventOverlapWithBuckets(
    db: AppDatabase,
    startDate: LocalDate,
    endDate: LocalDate?,
    startTime: LocalTime,
    endTime: LocalTime,
    recurFreq: RecurrenceFrequency,
    recurRule: RecurrenceRule
): OverlapInfo {
    val today = LocalDate.now()

    // Calculate end limit for checking
    val endLimit = endDate?.let {
        if (it.isBefore(today.plusMonths(generationMonths.toLong()))) it
        else today.plusMonths(generationMonths.toLong())
    } ?: today.plusMonths(generationMonths.toLong())

    var current = if (startDate.isBefore(today)) today else startDate

    // Check each date where the event would occur
    while (!current.isAfter(endLimit)) {
        val matchesRule = when (recurFreq) {
            RecurrenceFrequency.NONE -> current == startDate
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> recurRule.daysOfWeek?.contains(current.dayOfWeek.value) ?: true
            RecurrenceFrequency.MONTHLY -> recurRule.daysOfMonth?.contains(current.dayOfMonth) ?: true
            RecurrenceFrequency.YEARLY -> recurRule.monthAndDay?.let {
                it.first == current.dayOfMonth && it.second == current.monthValue
            } ?: true
        }

        if (matchesRule) {
            // Get all task bucket occurrences on this date
            val bucketsOnDate = db.taskBucketDao().getOccurrencesByDate(current)

            // Check for time overlap with any bucket
            for (bucket in bucketsOnDate) {
                if (doTimeRangesOverlap(startTime, endTime, bucket.startTime, bucket.endTime)) {
                    return OverlapInfo(
                        hasOverlap = true,
                        conflictType = "Task Bucket",
                        conflictDate = current,
                        conflictStartTime = bucket.startTime,
                        conflictEndTime = bucket.endTime
                    )
                }
            }
        }

        // Increment current date
        current = when (recurFreq) {
            RecurrenceFrequency.NONE,
            RecurrenceFrequency.DAILY,
            RecurrenceFrequency.WEEKLY,
            RecurrenceFrequency.MONTHLY -> current.plusDays(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }

    return OverlapInfo(hasOverlap = false)
}

/* Check if a new Task Bucket would overlap with existing Event occurrences */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun checkBucketOverlapWithEvents(
    db: AppDatabase,
    startDate: LocalDate,
    endDate: LocalDate?,
    startTime: LocalTime,
    endTime: LocalTime,
    recurFreq: RecurrenceFrequency,
    recurRule: RecurrenceRule
): OverlapInfo {
    val today = LocalDate.now()

    // Calculate end limit for checking
    val endLimit = endDate?.let {
        if (it.isBefore(today.plusMonths(generationMonths.toLong()))) it
        else today.plusMonths(generationMonths.toLong())
    } ?: today.plusMonths(generationMonths.toLong())

    var current = if (startDate.isBefore(today)) today else startDate

    // Get all event occurrences
    val allEventOccurrences = db.eventDao().getAllOccurrences()

    // Check each date where the bucket would occur
    while (!current.isAfter(endLimit)) {
        val matchesRule = when (recurFreq) {
            RecurrenceFrequency.NONE -> current == startDate
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> recurRule.daysOfWeek?.contains(current.dayOfWeek.value) ?: true
            RecurrenceFrequency.MONTHLY -> recurRule.daysOfMonth?.contains(current.dayOfMonth) ?: true
            RecurrenceFrequency.YEARLY -> recurRule.monthAndDay?.let {
                it.first == current.dayOfMonth && it.second == current.monthValue
            } ?: true
        }

        if (matchesRule) {
            // Filter events that occur on this date
            val eventsOnDate = allEventOccurrences.filter { it.occurDate == current }

            // Check for time overlap with any event
            for (event in eventsOnDate) {
                if (doTimeRangesOverlap(startTime, endTime, event.startTime, event.endTime)) {
                    return OverlapInfo(
                        hasOverlap = true,
                        conflictType = "Event",
                        conflictDate = current,
                        conflictStartTime = event.startTime,
                        conflictEndTime = event.endTime
                    )
                }
            }
        }

        // Increment current date
        current = when (recurFreq) {
            RecurrenceFrequency.NONE,
            RecurrenceFrequency.DAILY,
            RecurrenceFrequency.WEEKLY,
            RecurrenceFrequency.MONTHLY -> current.plusDays(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }

    return OverlapInfo(hasOverlap = false)
}

/* Format overlap info */
@RequiresApi(Build.VERSION_CODES.O)
fun formatOverlapMessage(overlapInfo: OverlapInfo): String {
    if (!overlapInfo.hasOverlap) return ""

    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val dateStr = overlapInfo.conflictDate?.format(dateFormatter) ?: ""
    val timeStr = overlapInfo.conflictStartTime?.format(timeFormatter) ?: ""

    return "Timing clashes with ${overlapInfo.conflictType} on\n $dateStr at $timeStr"
}
//</editor-fold>