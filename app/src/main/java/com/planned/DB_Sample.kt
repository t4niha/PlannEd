package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/* LOAD SAMPLE OBJECTS INTO DATABASE */
@RequiresApi(Build.VERSION_CODES.O)
fun runSample(db: AppDatabase) = runBlocking {

    // Determine week bounds based on settings
    val startOnMonday = SettingsManager.settings?.startWeekOnMonday ?: false
    val firstDayOfWeek = if (startOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val lastDayOfWeek = if (startOnMonday) DayOfWeek.SUNDAY else DayOfWeek.SATURDAY

    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val weekEnd = today.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))

    // Specific days this week
    val thisSunday    = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val thisMonday    = thisSunday.plusDays(1)
    val thisTuesday   = thisSunday.plusDays(2)
    val thisThursday  = thisSunday.plusDays(4)
    val thisSaturday  = thisSunday.plusDays(6)

    // CATEGORIES

    CategoryManager.insert(db, "School", "Academic classes and coursework", Preset12)
    val schoolCatId = db.categoryDao().getAll().last().id

    CategoryManager.insert(db, "Extracurricular", "Band, piano, and other activities", Preset5)
    val ecCatId = db.categoryDao().getAll().last().id

    CategoryManager.insert(db, "Home", "Household tasks and personal errands", Preset9)
    val homeCatId = db.categoryDao().getAll().last().id

    // EVENTS

    EventManager.insert(
        db = db,
        title = "English Class",
        notes = "Prof. Harrison — Room 204",
        color = null,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(12, 30),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.SUNDAY.value, DayOfWeek.TUESDAY.value)),
        categoryId = schoolCatId
    )
    val englishEventId = db.eventDao().getAllMasterEvents().last().id

    EventManager.insert(
        db = db,
        title = "Science Class",
        notes = "Prof. Chen — Room 112",
        color = null,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(12, 30),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value)),
        categoryId = schoolCatId
    )
    val scienceEventId = db.eventDao().getAllMasterEvents().last().id

    EventManager.insert(
        db = db,
        title = "Math Class",
        notes = "Prof. Williams — Room 308",
        color = null,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(12, 40),
        endTime = LocalTime.of(14, 10),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.SUNDAY.value, DayOfWeek.TUESDAY.value)),
        categoryId = schoolCatId
    )
    val mathEventId = db.eventDao().getAllMasterEvents().last().id

    EventManager.insert(
        db = db,
        title = "Social Studies Class",
        notes = "Prof. Okafor — Room 215",
        color = null,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(12, 40),
        endTime = LocalTime.of(14, 10),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value)),
        categoryId = schoolCatId
    )

    EventManager.insert(
        db = db,
        title = "Band Practice",
        notes = "Main Auditorium",
        color = null,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(12, 40),
        endTime = LocalTime.of(14, 10),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.THURSDAY.value)),
        categoryId = ecCatId
    )

    EventManager.insert(
        db = db,
        title = "Piano Lesson",
        notes = "Music Room 3",
        color = null,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(14, 10),
        endTime = LocalTime.of(15, 40),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.THURSDAY.value)),
        categoryId = ecCatId
    )

    EventManager.insert(
        db = db,
        title = "Meal Prep",
        notes = "Cook rice, chicken, and veggies",
        color = null,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(18, 30),
        endTime = LocalTime.of(20, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.THURSDAY.value)),
        categoryId = homeCatId
    )

    // REMINDERS

    ReminderManager.insert(
        db = db,
        title = "Doctor's Appointment",
        notes = "Get sore throat checked out",
        startDate = thisMonday,
        endDate = null,
        time = LocalTime.of(14, 30),
        allDay = false,
        recurFreq = RecurrenceFrequency.NONE,
        recurRule = RecurrenceRule(),
        categoryId = homeCatId
    )

    ReminderManager.insert(
        db = db,
        title = "Visit Counsellor's Office",
        notes = "Discuss next semester's classes",
        startDate = thisThursday,
        endDate = null,
        time = null,
        allDay = true,
        recurFreq = RecurrenceFrequency.NONE,
        recurRule = RecurrenceRule(),
        categoryId = schoolCatId
    )

    // DEADLINES

    DeadlineManager.insert(
        db = db,
        title = "Math Quiz",
        notes = "Chapter 3 Integrals",
        date = thisTuesday,
        time = LocalTime.of(12, 40),
        categoryId = schoolCatId,
        eventId = mathEventId
    )

    DeadlineManager.insert(
        db = db,
        title = "Shakespeare Essay",
        notes = "Upload on Canvas",
        date = thisThursday,
        time = LocalTime.of(20, 0),
        categoryId = schoolCatId,
        eventId = englishEventId
    )

    DeadlineManager.insert(
        db = db,
        title = "Chemistry Homework",
        notes = "Upload on Canvas",
        date = thisSaturday,
        time = LocalTime.of(20, 0),
        categoryId = schoolCatId,
        eventId = scienceEventId
    )

    // TASK BUCKETS

    TaskBucketManager.insert(
        db = db,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(16, 0),
        endTime = LocalTime.of(18, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(
            DayOfWeek.SUNDAY.value,
            DayOfWeek.MONDAY.value,
            DayOfWeek.TUESDAY.value,
            DayOfWeek.WEDNESDAY.value,
            DayOfWeek.THURSDAY.value
        ))
    )

    TaskBucketManager.insert(
        db = db,
        startDate = weekStart,
        endDate = weekEnd,
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(17, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(
            DayOfWeek.FRIDAY.value,
            DayOfWeek.SATURDAY.value
        ))
    )

    // ALL-DAY TASKS

    TaskManager.insert(
        db = db,
        title = "Pick up groceries",
        notes = "Bread, Milk, Eggs",
        allDay = thisMonday,
        breakable = false,
        startDate = null,
        startTime = null,
        predictedDuration = 60,
        categoryId = homeCatId,
        eventId = null,
        deadlineId = null,
        dependencyTaskId = null
    )

    TaskManager.insert(
        db = db,
        title = "Clean bedroom",
        notes = "Change bedsheets, organize closet",
        allDay = thisThursday,
        breakable = false,
        startDate = null,
        startTime = null,
        predictedDuration = 60,
        categoryId = homeCatId,
        eventId = null,
        deadlineId = null,
        dependencyTaskId = null
    )

    // MANUAL TASKS

    TaskManager.insert(
        db = db,
        title = "Read Hamlet",
        notes = "Pages 34–150",
        allDay = null,
        breakable = false,
        startDate = thisThursday,
        startTime = LocalTime.of(19, 0),
        predictedDuration = 60,
        categoryId = schoolCatId,
        eventId = null,
        deadlineId = null,
        dependencyTaskId = null
    )
}