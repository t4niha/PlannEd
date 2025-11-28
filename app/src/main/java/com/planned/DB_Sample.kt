package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime

/* LOAD SAMPLE OBJECTS INTO DATABASE */
@RequiresApi(Build.VERSION_CODES.O)
fun runSample(db: AppDatabase) = runBlocking {

    // Create category
    val catSchool = Category(
        title = "School",
        notes = "All academic related items",
        color = "Preset1"
    )
    db.categoryDao().insert(catSchool)
    val catId = db.categoryDao().getAll().last().id

    // Create master event
    val mathEvent = MasterEvent(
        title = "Math Lecture",
        notes = "Unit 3: Integrals",
        color = "Preset4",
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 15),
        recurFreq = RecurrenceFrequency.NONE,
        recurRule = RecurrenceRule(),
        categoryId = catId
    )
    db.eventDao().insert(mathEvent)
    val mathEventId = db.eventDao().getAllMasterEvents().last().id

    // Generate event occurrences
    val mathOccurrences = generateEventOccurrences(mathEvent).map { it.copy(masterEventId = mathEventId) }
    mathOccurrences.forEach { db.eventDao().insertOccurrence(it) }

    // Create deadline
    val assignmentDeadline = Deadline(
        title = "Math HW #4",
        notes = "Submit via Canvas",
        date = LocalDate.now().plusDays(2),
        time = LocalTime.of(23, 59),
        categoryId = catId,
        eventId = mathEventId
    )
    db.deadlineDao().insert(assignmentDeadline)
    val deadlineId = db.deadlineDao().getAll().last().id

    // Create master task bucket
    val studyBucket = MasterTaskBucket(
        startDate = LocalDate.now(),
        endDate = null,
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(16, 0),
        recurFreq = RecurrenceFrequency.NONE,
        recurRule = RecurrenceRule()
    )
    db.taskBucketDao().insert(studyBucket)
    val bucketId = db.taskBucketDao().getAllMasterBuckets().last().id

    // Generate task bucket occurrences
    val studyBucketOccurrences = generateTaskBucketOccurrences(studyBucket).map { it.copy(masterBucketId = bucketId) }
    studyBucketOccurrences.forEach { db.taskBucketDao().insertOccurrence(it) }

    // Create master task
    val readChapterTask = MasterTask(
        title = "Read Chapter 5",
        notes = "Pages 120–145",
        priority = 3,
        breakable = false,
        noIntervals = 1,
        startDate = LocalDate.now(),
        startTime = LocalTime.of(17, 0),
        predictedDuration = 60,
        status = 1,
        bucketId = bucketId,
        eventId = null,
        deadlineId = deadlineId,
        categoryId = catId
    )
    db.taskDao().insert(readChapterTask)
    val taskId = db.taskDao().getAllMasterTasks().last().id

    // Create task interval
    val readChapterInterval = TaskInterval(
        masterTaskId = taskId,
        intervalNo = 1,
        notes = "Focus on solving examples",
        occurDate = LocalDate.now(),
        startTime = LocalTime.of(17, 0),
        endTime = LocalTime.of(18, 0),
        status = 1
    )
    db.taskDao().insertInterval(readChapterInterval)

    // Create reminder
    val reminder = Reminder(
        title = "Buy graph paper",
        notes = "Needed for tomorrow's lab",
        color = "Preset3",
        date = LocalDate.now().plusDays(1),
        time = LocalTime.of(9, 0),
        allDay = false
    )
    db.reminderDao().insert(reminder)
}