package com.planned

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/* LOAD SAMPLE OBJECTS INTO DATABASE */
@RequiresApi(Build.VERSION_CODES.O)
fun runSample(context: Context, db: AppDatabase) = runBlocking {

    val startOnMonday = SettingsManager.settings?.startWeekOnMonday ?: false
    val firstDayOfWeek = if (startOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val lastDayOfWeek = if (startOnMonday) DayOfWeek.SUNDAY else DayOfWeek.SATURDAY

    val today = LocalDate.now().plusDays(7)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val weekEnd = today.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))

    val thisSunday    = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val thisMonday    = thisSunday.plusDays(1)
    val thisTuesday   = thisSunday.plusDays(2)
    val thisThursday  = thisSunday.plusDays(4)
    val thisSaturday  = thisSunday.plusDays(6)

    // CATEGORIES

    val collegeCatId = CategoryManager.insert(db, "College", "University classes and coursework", Preset12)
    val ecCatId      = CategoryManager.insert(db, "Extracurricular", "Band, piano, and other activities", Preset5)
    val homeCatId    = CategoryManager.insert(db, "Home", "Household tasks and personal errands", Preset9)

    // ACTIVE COURSES

    val mat350Id = db.courseDao().insert(Course(
        title = "Engineering Mathematics", courseCode = "MAT350",
        description = "Room SAC315 Prof MHM", credits = 3,
        year = 2026, semester = 1,
        weightQuiz = 20f, weightMid = 25f, weightAssignment = 10f,
        weightAttendance = 10f, weightFinal = 35f
    )).toInt()

    val phy108Id = db.courseDao().insert(Course(
        title = "Physics II", courseCode = "PHY108",
        description = "Room SAC209 Prof OIS", credits = 4,
        year = 2026, semester = 1,
        weightQuiz = 20f, weightMid = 25f, weightAssignment = 10f,
        weightAttendance = 10f, weightFinal = 35f
    )).toInt()

    val phy108LId = db.courseDao().insert(Course(
        title = "Physics II Lab", courseCode = "PHY108L",
        description = "Room SAC512 Prof RLS", credits = 1,
        year = 2026, semester = 1,
        weightQuiz = 20f, weightMid = 25f, weightAssignment = 10f,
        weightAttendance = 10f, weightFinal = 35f
    )).toInt()

    val eee141Id = db.courseDao().insert(Course(
        title = "Electrical Circuits I", courseCode = "EEE141",
        description = "Room SAC301 Prof KMR", credits = 3,
        year = 2026, semester = 1,
        weightQuiz = 20f, weightMid = 25f, weightAssignment = 10f,
        weightAttendance = 10f, weightFinal = 35f
    )).toInt()

    val eee141LId = db.courseDao().insert(Course(
        title = "Electrical Circuits I Lab", courseCode = "EEE141L",
        description = "Room SAC418 Prof KMR", credits = 1,
        year = 2026, semester = 1,
        weightQuiz = 20f, weightMid = 25f, weightAssignment = 10f,
        weightAttendance = 10f, weightFinal = 35f
    )).toInt()

    val cse311Id = db.courseDao().insert(Course(
        title = "Database Systems", courseCode = "CSE311",
        description = "Room SAC203 Prof ANR", credits = 3,
        year = 2026, semester = 1,
        weightQuiz = 20f, weightMid = 25f, weightAssignment = 10f,
        weightAttendance = 10f, weightFinal = 35f
    )).toInt()

    // EVENTS

    val engMathEventId = EventManager.insert(
        context = context, db = db,
        title = "Engineering Math Class",
        notes = "Prof. MHM — Room SAC315",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 30),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.SUNDAY.value, DayOfWeek.TUESDAY.value)),
        categoryId = collegeCatId, courseId = mat350Id
    )

    val phy2EventId = EventManager.insert(
        context = context, db = db,
        title = "Physics II Class",
        notes = "Prof. OIS — Room SAC209",
        color = Preset2,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(9, 20), endTime = LocalTime.of(10, 50),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.SUNDAY.value, DayOfWeek.TUESDAY.value)),
        categoryId = collegeCatId, courseId = phy108Id
    )

    val phy2LabEventId = EventManager.insert(
        context = context, db = db,
        title = "Physics II Lab",
        notes = "Prof. RLS — Room SAC512",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(9, 20), endTime = LocalTime.of(10, 50),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value)),
        categoryId = collegeCatId, courseId = phy108LId
    )

    val circuitsEventId = EventManager.insert(
        context = context, db = db,
        title = "Electrical Circuits Class",
        notes = "Prof. KMR — Room SAC301",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 30),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value)),
        categoryId = collegeCatId, courseId = eee141Id
    )

    val circuitsLabEventId = EventManager.insert(
        context = context, db = db,
        title = "Electrical Circuits Lab",
        notes = "Prof. KMR — Room SAC418",
        color = Preset2,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(12, 40), endTime = LocalTime.of(14, 10),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.SUNDAY.value, DayOfWeek.TUESDAY.value)),
        categoryId = collegeCatId, courseId = eee141LId
    )

    val dbEventId = EventManager.insert(
        context = context, db = db,
        title = "Database Systems Class",
        notes = "Prof. ANR — Room SAC203",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(12, 40), endTime = LocalTime.of(14, 10),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value)),
        categoryId = collegeCatId, courseId = cse311Id
    )

    val bandPracticeEventId = EventManager.insert(
        context = context, db = db,
        title = "Band Practice",
        notes = "Main Auditorium",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(12, 40), endTime = LocalTime.of(14, 10),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.THURSDAY.value)),
        categoryId = ecCatId
    )

    val pianoLessonEventId = EventManager.insert(
        context = context, db = db,
        title = "Piano Lesson",
        notes = "Music Room 3",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(14, 20), endTime = LocalTime.of(15, 50),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.THURSDAY.value)),
        categoryId = ecCatId
    )

    EventManager.insert(
        context = context, db = db,
        title = "Morning Yoga",
        notes = "15m stretch, 30m flow sequence, 15m cool down",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 0),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.THURSDAY.value)),
        categoryId = homeCatId
    )

    EventManager.insert(
        context = context, db = db,
        title = "Meal Prep",
        notes = "Cook rice, chicken, and veggies",
        color = null,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(18, 15), endTime = LocalTime.of(19, 45),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.THURSDAY.value)),
        categoryId = homeCatId
    )

    // REMINDERS

    ReminderManager.insert(
        context = context, db = db,
        title = "Doctor's Appointment",
        notes = "Get sore throat checked out",
        startDate = thisMonday, endDate = null,
        time = LocalTime.of(14, 30), allDay = false,
        recurFreq = RecurrenceFrequency.NONE, recurRule = RecurrenceRule(),
        categoryId = homeCatId
    )

    ReminderManager.insert(
        context = context, db = db,
        title = "Visit Counsellor's Office",
        notes = "Discuss next semester's classes",
        startDate = thisThursday, endDate = null,
        time = null, allDay = true,
        recurFreq = RecurrenceFrequency.NONE, recurRule = RecurrenceRule(),
        categoryId = collegeCatId
    )

    // DEADLINES

    val phy2QuizDeadlineId = DeadlineManager.insert(
        context = context, db = db,
        title = "Physics II Quiz",
        notes = "Chapter 3 Electric Fields",
        date = thisTuesday, time = LocalTime.of(12, 40),
        categoryId = collegeCatId, eventId = phy2EventId, courseId = phy108Id
    )

    val circuitsAssignmentId = DeadlineManager.insert(
        context = context, db = db,
        title = "Circuits Assignment",
        notes = "Upload on Canvas",
        date = thisThursday, time = LocalTime.of(20, 0),
        categoryId = collegeCatId, eventId = circuitsEventId, courseId = eee141Id
    )

    DeadlineManager.insert(
        context = context, db = db,
        title = "Database Systems Homework",
        notes = "Upload on Canvas",
        date = thisSaturday, time = LocalTime.of(20, 0),
        categoryId = collegeCatId, eventId = dbEventId, courseId = cse311Id
    )

    // TASK BUCKETS

    TaskBucketManager.insert(
        context = context, db = db,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(16, 0), endTime = LocalTime.of(18, 45),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(
            DayOfWeek.MONDAY.value, DayOfWeek.TUESDAY.value,
            DayOfWeek.WEDNESDAY.value, DayOfWeek.THURSDAY.value
        ))
    )

    TaskBucketManager.insert(
        context = context, db = db,
        startDate = weekStart, endDate = weekEnd,
        startTime = LocalTime.of(14, 0), endTime = LocalTime.of(17, 15),
        recurFreq = RecurrenceFrequency.WEEKLY,
        recurRule = RecurrenceRule(daysOfWeek = listOf(DayOfWeek.FRIDAY.value))
    )

    // ALL-DAY TASKS

    TaskManager.insert(
        context = context, db = db,
        title = "Pick up groceries",
        notes = "Bread, Milk, Eggs",
        allDay = thisMonday, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = homeCatId, eventId = null, deadlineId = null, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Clean bedroom",
        notes = "Change bedsheets, organize closet",
        allDay = thisThursday, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = homeCatId, eventId = null, deadlineId = null, dependencyTaskId = null
    )

    // MANUAL TASKS

    TaskManager.insert(
        context = context, db = db,
        title = "Review Circuit Diagrams",
        notes = "Go over lecture slides from last week",
        allDay = null, breakable = false,
        startDate = thisThursday, startTime = LocalTime.of(19, 30), predictedDuration = 60,
        categoryId = collegeCatId, eventId = circuitsEventId, deadlineId = null, dependencyTaskId = null
    )

    // AUTO-SCHEDULED TASKS

    TaskManager.insert(
        context = context, db = db,
        title = "Engineering Math Problem Set",
        notes = "Ch. 3 exercise 3.4 Q 1-20",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = collegeCatId, eventId = engMathEventId, deadlineId = null, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Rehearse Setlist",
        notes = "Run through full band setlist on beat",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = ecCatId, eventId = bandPracticeEventId, deadlineId = null, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Circuits Assignment",
        notes = "KVL and KCL practice problems",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = collegeCatId, eventId = circuitsEventId,
        deadlineId = circuitsAssignmentId, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Make Reading List",
        notes = "3 books to read over the vacation",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = null, eventId = null, deadlineId = null, dependencyTaskId = null
    )

    val copyPhyNotesId = TaskManager.insert(
        context = context, db = db,
        title = "Copy Physics Notes",
        notes = "Get notes from Radia on missed classes",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = collegeCatId, eventId = phy2EventId, deadlineId = null, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Database Systems ER Diagram",
        notes = "Design entity-relationship diagram for assignment",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = collegeCatId, eventId = dbEventId, deadlineId = null, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Review Finances",
        notes = "Go over recent purchases and update budget",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = null, eventId = null, deadlineId = null, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Practice Staccato Swap",
        notes = "Drill staccato hand transitions from lesson book",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = ecCatId, eventId = pianoLessonEventId, deadlineId = null, dependencyTaskId = null
    )

    TaskManager.insert(
        context = context, db = db,
        title = "Study Electric Fields",
        notes = "Ch. 3 pages 68-91",
        allDay = null, breakable = false,
        startDate = null, startTime = null, predictedDuration = 60,
        categoryId = collegeCatId, eventId = phy2EventId,
        deadlineId = phy2QuizDeadlineId, dependencyTaskId = copyPhyNotesId
    )

    generateTaskIntervals(context, db)

    // COMPLETED TASKS

    val lastWeekMonday = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Engineering Math tasks
    db.taskDao().insert(MasterTask(
        title = "Eng Math Problem Set 1", noIntervals = 0,
        predictedDuration = 45, overTime = 20, deadlineMissed = false,
        status = 3, categoryId = collegeCatId, eventId = engMathEventId,
        completedAt = lastWeekMonday.atTime(17, 5)
    ))
    db.taskDao().insert(MasterTask(
        title = "Eng Math Problem Set 2", noIntervals = 0,
        predictedDuration = 60, overTime = 35, deadlineMissed = true,
        status = 3, categoryId = collegeCatId, eventId = engMathEventId,
        completedAt = lastWeekMonday.plusDays(2).atTime(18, 35)
    ))
    db.taskDao().insert(MasterTask(
        title = "Eng Math Problem Set 3", noIntervals = 0,
        predictedDuration = 30, overTime = 25, deadlineMissed = true,
        status = 3, categoryId = collegeCatId, eventId = engMathEventId,
        completedAt = lastWeekMonday.plusDays(4).atTime(16, 55)
    ))

    // Electrical Circuits tasks
    db.taskDao().insert(MasterTask(
        title = "Circuits Assignment 1", noIntervals = 0,
        predictedDuration = 90, overTime = 15, deadlineMissed = false,
        status = 3, categoryId = collegeCatId, eventId = circuitsEventId,
        completedAt = lastWeekMonday.plusDays(1).atTime(17, 45)
    ))
    db.taskDao().insert(MasterTask(
        title = "Circuits Assignment 2", noIntervals = 0,
        predictedDuration = 75, overTime = 0, deadlineMissed = false,
        status = 3, categoryId = collegeCatId, eventId = circuitsEventId,
        completedAt = lastWeekMonday.plusDays(3).atTime(16, 15)
    ))
    db.taskDao().insert(MasterTask(
        title = "Circuits Assignment 3", noIntervals = 0,
        predictedDuration = 120, overTime = 30, deadlineMissed = true,
        status = 3, categoryId = collegeCatId, eventId = circuitsEventId,
        completedAt = lastWeekMonday.plusDays(5).atTime(19, 30)
    ))

    // Piano Lesson tasks
    db.taskDao().insert(MasterTask(
        title = "Piano Practice 1", noIntervals = 0,
        predictedDuration = 30, overTime = 5, deadlineMissed = false,
        status = 3, categoryId = ecCatId, eventId = pianoLessonEventId,
        completedAt = lastWeekMonday.atTime(16, 35)
    ))
    db.taskDao().insert(MasterTask(
        title = "Piano Practice 2", noIntervals = 0,
        predictedDuration = 25, overTime = 0, deadlineMissed = false,
        status = 3, categoryId = ecCatId, eventId = pianoLessonEventId,
        completedAt = lastWeekMonday.plusDays(2).atTime(17, 25)
    ))
    db.taskDao().insert(MasterTask(
        title = "Piano Practice 3", noIntervals = 0,
        predictedDuration = 20, overTime = 10, deadlineMissed = false,
        status = 3, categoryId = ecCatId, eventId = pianoLessonEventId,
        completedAt = lastWeekMonday.plusDays(4).atTime(16, 30)
    ))

    // Home tasks
    db.taskDao().insert(MasterTask(
        title = "Clean Kitchen", noIntervals = 0,
        predictedDuration = 45, overTime = 15, deadlineMissed = false,
        status = 3, categoryId = homeCatId, eventId = null,
        completedAt = lastWeekMonday.plusDays(1).atTime(18, 0)
    ))
    db.taskDao().insert(MasterTask(
        title = "Laundry", noIntervals = 0,
        predictedDuration = 30, overTime = 0, deadlineMissed = false,
        status = 3, categoryId = homeCatId, eventId = null,
        completedAt = lastWeekMonday.plusDays(3).atTime(15, 30)
    ))
    db.taskDao().insert(MasterTask(
        title = "Grocery Run", noIntervals = 0,
        predictedDuration = 60, overTime = 20, deadlineMissed = false,
        status = 3, categoryId = homeCatId, eventId = null,
        completedAt = lastWeekMonday.plusDays(5).atTime(17, 20)
    ))

    // PATCH ATI RECORDS

    db.categoryATIDao().getById(collegeCatId)?.let {
        db.categoryATIDao().update(it.copy(
            tasksCompleted = 6,
            deadlineMissCount = 3,
            avgOvertime = 20.8333f,
            predictedPadding = 20,
            paddingSlope = -0.009524f,
            paddingIntercept = 21.5f,
            score = 0.3633f
        ))
    }
    db.categoryATIDao().getById(ecCatId)?.let {
        db.categoryATIDao().update(it.copy(
            tasksCompleted = 3,
            deadlineMissCount = 0,
            avgOvertime = 5.0f,
            predictedPadding = 5,
            paddingSlope = -0.5f,
            paddingIntercept = 17.5f,
            score = 0.2125f
        ))
    }
    db.categoryATIDao().getById(homeCatId)?.let {
        db.categoryATIDao().update(it.copy(
            tasksCompleted = 3,
            deadlineMissCount = 0,
            avgOvertime = 11.6667f,
            predictedPadding = 15,
            paddingSlope = 0.6667f,
            paddingIntercept = -18.3333f,
            score = 0.2162f
        ))
    }

    db.eventATIDao().getById(engMathEventId)?.let {
        db.eventATIDao().update(it.copy(
            tasksCompleted = 3,
            deadlineMissCount = 2,
            avgOvertime = 26.6667f,
            predictedPadding = 30,
            paddingSlope = 0.3333f,
            paddingIntercept = 11.6667f,
            score = 0.4422f
        ))
    }
    db.eventATIDao().getById(circuitsEventId)?.let {
        db.eventATIDao().update(it.copy(
            tasksCompleted = 3,
            deadlineMissCount = 1,
            avgOvertime = 15.0f,
            predictedPadding = 15,
            paddingSlope = 0.6429f,
            paddingIntercept = -46.0714f,
            score = 0.3244f
        ))
    }
    db.eventATIDao().getById(pianoLessonEventId)?.let {
        db.eventATIDao().update(it.copy(
            tasksCompleted = 3,
            deadlineMissCount = 0,
            avgOvertime = 5.0f,
            predictedPadding = 5,
            paddingSlope = -0.5f,
            paddingIntercept = 17.5f,
            score = 0.2125f
        ))
    }
    db.eventATIDao().getById(0)?.let {
        db.eventATIDao().update(it.copy(
            tasksCompleted = 3,
            deadlineMissCount = 0,
            avgOvertime = 11.6667f,
            predictedPadding = 15,
            paddingSlope = 0.6667f,
            paddingIntercept = -18.3333f,
            score = 0.2162f
        ))
    }

    // GRADING SCALE

    db.gradingScaleDao().insert(GradingScale(
        id = 0, cgpa = 0f,
        gpaAPlus = 4.0f, gpaA = 4.0f, gpaAMinus = 3.7f,
        gpaBPlus = 3.3f, gpaB = 3.0f, gpaBMinus = 2.7f,
        gpaCPlus = 2.3f, gpaC = 2.0f, gpaCMinus = 1.7f,
        gpaDPlus = 1.3f, gpaD = 1.0f, gpaDMinus = 0.7f,
        gpaF = 0.0f, gpaU = null, gpaP = null,
        gpaS = null, gpaW = null, gpaI = null,
        gpaN = null, gpaNp = null, gpaNC = null
    ))

    // GRADE ITEMS

    // MAT350
    db.gradeItemDao().insert(GradeItem(courseId = mat350Id, type = GradeItemType.QUIZ, title = "1", marksReceived = 7f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = mat350Id, type = GradeItemType.QUIZ, title = "2", marksReceived = 8f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = mat350Id, type = GradeItemType.MID, title = "", marksReceived = 15f, totalMarks = 20f))

    // PHY108
    db.gradeItemDao().insert(GradeItem(courseId = phy108Id, type = GradeItemType.QUIZ, title = "1", marksReceived = 6f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = phy108Id, type = GradeItemType.QUIZ, title = "2", marksReceived = 9f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = phy108Id, type = GradeItemType.MID, title = "", marksReceived = 13f, totalMarks = 20f))

    // PHY108L
    db.gradeItemDao().insert(GradeItem(courseId = phy108LId, type = GradeItemType.QUIZ, title = "1", marksReceived = 8f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = phy108LId, type = GradeItemType.QUIZ, title = "2", marksReceived = 7f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = phy108LId, type = GradeItemType.MID, title = "", marksReceived = 16f, totalMarks = 20f))

    // EEE141
    db.gradeItemDao().insert(GradeItem(courseId = eee141Id, type = GradeItemType.QUIZ, title = "1", marksReceived = 7f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = eee141Id, type = GradeItemType.QUIZ, title = "2", marksReceived = 6f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = eee141Id, type = GradeItemType.MID, title = "", marksReceived = 12f, totalMarks = 20f))

    // EEE141L
    db.gradeItemDao().insert(GradeItem(courseId = eee141LId, type = GradeItemType.QUIZ, title = "1", marksReceived = 9f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = eee141LId, type = GradeItemType.QUIZ, title = "2", marksReceived = 8f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = eee141LId, type = GradeItemType.MID, title = "", marksReceived = 17f, totalMarks = 20f))

    // CSE311
    db.gradeItemDao().insert(GradeItem(courseId = cse311Id, type = GradeItemType.QUIZ, title = "1", marksReceived = 8f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = cse311Id, type = GradeItemType.QUIZ, title = "2", marksReceived = 9f, totalMarks = 10f))
    db.gradeItemDao().insert(GradeItem(courseId = cse311Id, type = GradeItemType.MID, title = "", marksReceived = 14f, totalMarks = 20f))

    // COMPLETED COURSES

    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Programming Language I", courseCode = "CSE115", credits = 3, year = 2025, semester = 1, calculatedGrade = 88f, submitGrade = "A"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Programming Language I Lab", courseCode = "CSE115L", credits = 1, year = 2025, semester = 1, calculatedGrade = 92f, submitGrade = "A+"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Introduction to Composition", courseCode = "ENG102", credits = 3, year = 2025, semester = 1, calculatedGrade = 79f, submitGrade = "B+"))

    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Calculus I", courseCode = "MAT120", credits = 3, year = 2025, semester = 2, calculatedGrade = 83f, submitGrade = "A-"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Discrete Mathematics", courseCode = "CSE173", credits = 3, year = 2025, semester = 2, calculatedGrade = 76f, submitGrade = "B+"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Introduction to Ethics", courseCode = "PHI104", credits = 3, year = 2025, semester = 2, calculatedGrade = 91f, submitGrade = "A"))

    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Calculus II", courseCode = "MAT130", credits = 3, year = 2025, semester = 3, calculatedGrade = 81f, submitGrade = "A-"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Data Structures and Algorithm", courseCode = "CSE225", credits = 3, year = 2025, semester = 3, calculatedGrade = 74f, submitGrade = "B"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Physics I", courseCode = "PHY107", credits = 4, year = 2025, semester = 3, calculatedGrade = 86f, submitGrade = "A"))

    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Linear Algebra", courseCode = "MAT125", credits = 3, year = 2026, semester = 4, calculatedGrade = 78f, submitGrade = "B+"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Programming Language II", courseCode = "CSE215", credits = 3, year = 2026, semester = 4, calculatedGrade = 85f, submitGrade = "A-"))
    db.completedCourseDao().insert(CompletedCourse(courseTitle = "Digital Logic Design", courseCode = "CSE231", credits = 3, year = 2026, semester = 4, calculatedGrade = 72f, submitGrade = "B"))

    generateTaskIntervals(context, db)
}