package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/* LOAD SAMPLE OBJECTS INTO DATABASE */
@RequiresApi(Build.VERSION_CODES.O)
fun runSample(db: AppDatabase) = runBlocking {

    // Category: School
    CategoryManager.insert(
        db = db,
        title = "School",
        notes = "All academic related",
        color = Preset1
    )
    val schoolCatId = db.categoryDao().getAll().last().id

    // Category: Soccer
    CategoryManager.insert(
        db = db,
        title = "Soccer",
        notes = "Soccer lessons",
        color = Preset5
    )
    val soccerCatId = db.categoryDao().getAll().last().id

    // Category: Tutoring
    CategoryManager.insert(
        db = db,
        title = "Tutoring",
        notes = "Tutoring sessions",
        color = Preset9
    )
    val tutoringCatId = db.categoryDao().getAll().last().id

    // Event: Math
    EventManager.insert(
        db = db,
        title = "Math Lecture",
        notes = "Calculus and linear algebra",
        color = Preset2,
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 30),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.MONDAY.value)),
        categoryId = schoolCatId
    )
    val mathEventId = db.eventDao().getAllMasterEvents().last().id

    // Event: Chemistry
    EventManager.insert(
        db = db,
        title = "Chemistry Lab",
        notes = "Organic chemistry experiments",
        color = Preset3,
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(10, 30),
        endTime = LocalTime.of(12, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.WEDNESDAY.value)),
        categoryId = schoolCatId
    )

    // Event: Physics
    EventManager.insert(
        db = db,
        title = "Physics Lecture",
        notes = "Mechanics and thermodynamics",
        color = Preset4,
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 30),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.FRIDAY.value)),
        categoryId = schoolCatId
    )

    // Math Quiz Deadline
    val nextMonday = LocalDate.now().with(DayOfWeek.MONDAY).let {
        if (it.isBefore(LocalDate.now()) || it.isEqual(LocalDate.now()))
            it.plusWeeks(1)
        else
            it
    }
    DeadlineManager.insert(
        db = db,
        title = "Math Quiz 3",
        notes = "Covers chapters 5-7",
        date = nextMonday,
        time = LocalTime.of(8, 45),
        categoryId = schoolCatId,
        eventId = mathEventId
    )

    // Task Bucket: Study Time
    TaskBucketManager.insert(
        db = db,
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(18, 0),
        endTime = LocalTime.of(20, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.MONDAY.value))
    )
    TaskBucketManager.insert(
        db = db,
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(18, 0),
        endTime = LocalTime.of(20, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.WEDNESDAY.value))
    )

    // Event: Soccer Practice
    EventManager.insert(
        db = db,
        title = "Soccer Practice",
        notes = "Team practice and drills",
        color = Preset6,
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(15, 0),
        endTime = LocalTime.of(17, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.SATURDAY.value)),
        categoryId = soccerCatId
    )

    // Reminder: Warm up
    ReminderManager.insert(
        db = db,
        title = "Warm up for soccer",
        notes = "Do stretches before practice",
        color = Preset7,
        startDate = LocalDate.now(),
        endDate = null,
        time = LocalTime.of(14, 30),
        allDay = false,
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.SATURDAY.value)),
        categoryId = soccerCatId
    )
}