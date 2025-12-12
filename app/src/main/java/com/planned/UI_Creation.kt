package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/* CREATION DETAILS */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Creation(db: AppDatabase) {
    val scrollState = rememberScrollState()
    var selectedType by remember { mutableStateOf("Task") }
    val scope = rememberCoroutineScope()

    // Validation notification
    var showValidationNotification by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    // Overlap notification
    var showOverlapNotification by remember { mutableStateOf(false) }
    var overlapMessage by remember { mutableStateOf("") }

    // Success notification
    var showSuccessNotification by remember { mutableStateOf(false) }

    // Reset trigger
    var resetTrigger by remember { mutableIntStateOf(0) }

    /* State variables */

    // Category
    var categoryTitle by remember { mutableStateOf("") }
    var categoryNotes by remember { mutableStateOf("") }
    var categoryColor by remember { mutableStateOf(Preset1) }

    // Event
    var eventTitle by remember { mutableStateOf("") }
    var eventNotes by remember { mutableStateOf("") }
    var eventColor by remember { mutableStateOf(Preset1) }
    var eventStartDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var eventEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var eventStartTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var eventEndTime by remember { mutableStateOf(LocalTime.of(11, 0)) }
    var eventRecurrenceFreq by remember { mutableStateOf(RecurrenceFrequency.NONE) }
    var eventSelectedDaysOfWeek by remember { mutableStateOf(setOf(7)) }
    var eventSelectedDaysOfMonth by remember { mutableStateOf(setOf(1)) }
    var eventSelectedCategory by remember { mutableStateOf<Int?>(null) }

    // Deadline
    var deadlineTitle by remember { mutableStateOf("") }
    var deadlineNotes by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var deadlineTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var deadlineSelectedCategory by remember { mutableStateOf<Int?>(null) }
    var deadlineSelectedEvent by remember { mutableStateOf<Int?>(null) }
    var deadlineAutoScheduleTask by remember { mutableStateOf(false) }
    var deadlineTaskPriority by remember { mutableIntStateOf(3) }
    var deadlineTaskDurationHours by remember { mutableIntStateOf(0) }
    var deadlineTaskDurationMinutes by remember { mutableIntStateOf(30) }
    var deadlineTaskIsBreakable by remember { mutableStateOf(false) }
    var deadlineTaskBreakableLockedByDuration by remember { mutableStateOf(false) }

    // Task Bucket
    var bucketStartDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var bucketEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var bucketStartTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var bucketEndTime by remember { mutableStateOf(LocalTime.of(11, 0)) }
    var bucketRecurrenceFreq by remember { mutableStateOf(RecurrenceFrequency.NONE) }
    var bucketSelectedDaysOfWeek by remember { mutableStateOf(setOf(7)) }
    var bucketSelectedDaysOfMonth by remember { mutableStateOf(setOf(1)) }

    // Task
    var taskTitle by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }
    var taskPriority by remember { mutableIntStateOf(3) }
    var taskIsBreakable by remember { mutableStateOf(false) }
    var taskIsAutoSchedule by remember { mutableStateOf(true) }
    var taskStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var taskStartTime by remember { mutableStateOf<LocalTime?>(null) }
    var taskDurationHours by remember { mutableIntStateOf(0) }
    var taskDurationMinutes by remember { mutableIntStateOf(30) }
    var taskSelectedCategory by remember { mutableStateOf<Int?>(null) }
    var taskSelectedEvent by remember { mutableStateOf<Int?>(null) }
    var taskSelectedDeadline by remember { mutableStateOf<Int?>(null) }
    var taskSelectedDependencyTask by remember { mutableStateOf<Int?>(null) }
    var taskBreakableLockedByDuration by remember { mutableStateOf(false) }

    // Reminder
    var reminderTitle by remember { mutableStateOf("") }
    var reminderNotes by remember { mutableStateOf("") }
    var reminderColor by remember { mutableStateOf(Preset1) }
    var reminderStartDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var reminderEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var reminderIsAllDay by remember { mutableStateOf(true) }
    var reminderTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var reminderRecurrenceFreq by remember { mutableStateOf(RecurrenceFrequency.NONE) }
    var reminderSelectedDaysOfWeek by remember { mutableStateOf(setOf(7)) }
    var reminderSelectedDaysOfMonth by remember { mutableStateOf(setOf(1)) }
    var reminderSelectedCategory by remember { mutableStateOf<Int?>(null) }

    // Clear all forms
    fun clearAllForms() {
        // Category
        categoryTitle = ""
        categoryNotes = ""
        categoryColor = Preset1

        // Event
        eventTitle = ""
        eventNotes = ""
        eventColor = Preset1
        eventStartDate = LocalDate.now().plusDays(1)
        eventEndDate = null
        eventStartTime = LocalTime.of(10, 0)
        eventEndTime = LocalTime.of(11, 0)
        eventRecurrenceFreq = RecurrenceFrequency.NONE
        eventSelectedDaysOfWeek = setOf(7)
        eventSelectedDaysOfMonth = setOf(1)
        eventSelectedCategory = null

        // Deadline
        deadlineTitle = ""
        deadlineNotes = ""
        deadlineDate = LocalDate.now().plusDays(1)
        deadlineTime = LocalTime.of(10, 0)
        deadlineSelectedCategory = null
        deadlineSelectedEvent = null
        deadlineAutoScheduleTask = false
        deadlineTaskPriority = 3
        deadlineTaskDurationHours = 0
        deadlineTaskDurationMinutes = 30
        deadlineTaskIsBreakable = false
        deadlineTaskBreakableLockedByDuration = false

        // Task Bucket
        bucketStartDate = LocalDate.now().plusDays(1)
        bucketEndDate = null
        bucketStartTime = LocalTime.of(10, 0)
        bucketEndTime = LocalTime.of(11, 0)
        bucketRecurrenceFreq = RecurrenceFrequency.NONE
        bucketSelectedDaysOfWeek = setOf(7)
        bucketSelectedDaysOfMonth = setOf(1)

        // Task
        taskTitle = ""
        taskNotes = ""
        taskPriority = 3
        taskIsBreakable = false
        taskIsAutoSchedule = true
        taskStartDate = null
        taskStartTime = null
        taskDurationHours = 0
        taskDurationMinutes = 30
        taskSelectedCategory = null
        taskSelectedEvent = null
        taskSelectedDeadline = null
        taskSelectedDependencyTask = null
        taskBreakableLockedByDuration = false

        // Reminder
        reminderTitle = ""
        reminderNotes = ""
        reminderColor = Preset1
        reminderStartDate = LocalDate.now().plusDays(1)
        reminderEndDate = null
        reminderIsAllDay = true
        reminderTime = LocalTime.of(10, 0)
        reminderRecurrenceFreq = RecurrenceFrequency.NONE
        reminderSelectedDaysOfWeek = setOf(7)
        reminderSelectedDaysOfMonth = setOf(1)
        reminderSelectedCategory = null

        // Increment reset trigger
        resetTrigger++

        // Scroll to top
        scope.launch {
            scrollState.animateScrollTo(0)
        }
    }

    // Clear all forms when Type changes
    LaunchedEffect(selectedType) {
        clearAllForms()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            // Type field
            selectedType = typePickerField(
                initialType = selectedType,
                key = resetTrigger
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Display form based on selected type
            when (selectedType) {
                "Task" -> {
                    TaskForm(
                        db = db,
                        title = taskTitle,
                        onTitleChange = { taskTitle = it },
                        notes = taskNotes,
                        onNotesChange = { taskNotes = it },
                        priority = taskPriority,
                        onPriorityChange = { taskPriority = it },
                        isBreakable = taskIsBreakable,
                        onBreakableChange = { taskIsBreakable = it },
                        isAutoSchedule = taskIsAutoSchedule,
                        startDate = taskStartDate,
                        startTime = taskStartTime,
                        onScheduleChange = { auto, date, time ->
                            taskIsAutoSchedule = auto
                            taskStartDate = if (auto) null else date
                            taskStartTime = if (auto) null else time
                        },
                        durationHours = taskDurationHours,
                        durationMinutes = taskDurationMinutes,
                        onDurationChange = { hours, mins ->
                            taskDurationHours = hours
                            taskDurationMinutes = mins
                        },
                        selectedCategory = taskSelectedCategory,
                        onCategoryChange = { taskSelectedCategory = it },
                        selectedEvent = taskSelectedEvent,
                        onEventChange = { taskSelectedEvent = it },
                        selectedDeadline = taskSelectedDeadline,
                        onDeadlineChange = { taskSelectedDeadline = it },
                        selectedDependencyTask = taskSelectedDependencyTask,
                        onDependencyTaskChange = { taskSelectedDependencyTask = it },
                        breakableLockedByDuration = taskBreakableLockedByDuration,
                        onBreakableLockedByDurationChange = { taskBreakableLockedByDuration = it },
                        resetTrigger = resetTrigger
                    )
                }
                "Reminder" -> {
                    ReminderForm(
                        db = db,
                        title = reminderTitle,
                        onTitleChange = { reminderTitle = it },
                        notes = reminderNotes,
                        onNotesChange = { reminderNotes = it },
                        color = reminderColor,
                        onColorChange = { reminderColor = it },
                        startDate = reminderStartDate,
                        onStartDateChange = { reminderStartDate = it },
                        endDate = reminderEndDate,
                        isAllDay = reminderIsAllDay,
                        time = reminderTime,
                        onAllDayTimeChange = { allDay, time ->
                            reminderIsAllDay = allDay
                            reminderTime = time
                        },
                        recurrenceFreq = reminderRecurrenceFreq,
                        selectedDaysOfWeek = reminderSelectedDaysOfWeek,
                        selectedDaysOfMonth = reminderSelectedDaysOfMonth,
                        onRecurrenceChange = { freq, daysWeek, daysMonth, endDateVal ->
                            reminderRecurrenceFreq = freq
                            reminderSelectedDaysOfWeek = daysWeek
                            reminderSelectedDaysOfMonth = daysMonth
                            reminderEndDate = endDateVal
                        },
                        selectedCategory = reminderSelectedCategory,
                        onCategoryChange = { reminderSelectedCategory = it },
                        resetTrigger = resetTrigger
                    )
                }
                "Deadline" -> {
                    DeadlineForm(
                        db = db,
                        title = deadlineTitle,
                        onTitleChange = { deadlineTitle = it },
                        notes = deadlineNotes,
                        onNotesChange = { deadlineNotes = it },
                        date = deadlineDate,
                        onDateChange = { deadlineDate = it },
                        time = deadlineTime,
                        onTimeChange = { deadlineTime = it },
                        selectedCategory = deadlineSelectedCategory,
                        onCategoryChange = { deadlineSelectedCategory = it },
                        selectedEvent = deadlineSelectedEvent,
                        onEventChange = { deadlineSelectedEvent = it },
                        autoScheduleTask = deadlineAutoScheduleTask,
                        onAutoScheduleTaskChange = { deadlineAutoScheduleTask = it },
                        taskPriority = deadlineTaskPriority,
                        onTaskPriorityChange = { deadlineTaskPriority = it },
                        taskDurationHours = deadlineTaskDurationHours,
                        taskDurationMinutes = deadlineTaskDurationMinutes,
                        onTaskDurationChange = { hours, mins ->
                            deadlineTaskDurationHours = hours
                            deadlineTaskDurationMinutes = mins
                        },
                        taskIsBreakable = deadlineTaskIsBreakable,
                        onTaskIsBreakableChange = { deadlineTaskIsBreakable = it },
                        taskBreakableLockedByDuration = deadlineTaskBreakableLockedByDuration,
                        onTaskBreakableLockedByDurationChange = { deadlineTaskBreakableLockedByDuration = it },
                        resetTrigger = resetTrigger
                    )
                }
                "Event" -> {
                    EventForm(
                        db = db,
                        title = eventTitle,
                        onTitleChange = { eventTitle = it },
                        notes = eventNotes,
                        onNotesChange = { eventNotes = it },
                        color = eventColor,
                        onColorChange = { eventColor = it },
                        startDate = eventStartDate,
                        onStartDateChange = { eventStartDate = it },
                        endDate = eventEndDate,
                        startTime = eventStartTime,
                        onStartTimeChange = { eventStartTime = it },
                        endTime = eventEndTime,
                        onEndTimeChange = { eventEndTime = it },
                        recurrenceFreq = eventRecurrenceFreq,
                        selectedDaysOfWeek = eventSelectedDaysOfWeek,
                        selectedDaysOfMonth = eventSelectedDaysOfMonth,
                        onRecurrenceChange = { freq, daysWeek, daysMonth, endDateVal ->
                            eventRecurrenceFreq = freq
                            eventSelectedDaysOfWeek = daysWeek
                            eventSelectedDaysOfMonth = daysMonth
                            eventEndDate = endDateVal
                        },
                        selectedCategory = eventSelectedCategory,
                        onCategoryChange = { eventSelectedCategory = it },
                        resetTrigger = resetTrigger
                    )
                }
                "Task Bucket" -> {
                    TaskBucketForm(
                        db = db,
                        startDate = bucketStartDate,
                        onStartDateChange = { bucketStartDate = it },
                        endDate = bucketEndDate,
                        startTime = bucketStartTime,
                        onStartTimeChange = { bucketStartTime = it },
                        endTime = bucketEndTime,
                        onEndTimeChange = { bucketEndTime = it },
                        recurrenceFreq = bucketRecurrenceFreq,
                        selectedDaysOfWeek = bucketSelectedDaysOfWeek,
                        selectedDaysOfMonth = bucketSelectedDaysOfMonth,
                        onRecurrenceChange = { freq, daysWeek, daysMonth, endDateVal ->
                            bucketRecurrenceFreq = freq
                            bucketSelectedDaysOfWeek = daysWeek
                            bucketSelectedDaysOfMonth = daysMonth
                            bucketEndDate = endDateVal
                        },
                        resetTrigger = resetTrigger
                    )
                }
                "Category" -> {
                    CategoryForm(
                        title = categoryTitle,
                        onTitleChange = { categoryTitle = it },
                        notes = categoryNotes,
                        onNotesChange = { categoryNotes = it },
                        color = categoryColor,
                        onColorChange = { categoryColor = it },
                        resetTrigger = resetTrigger
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Clear and Save buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        clearAllForms()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Clear",fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        // Validate and save based on selected type
                        when (selectedType) {
                            "Task" -> {
                                if (taskTitle.isBlank()) {
                                    validationMessage = "Title is required"
                                    showValidationNotification = true
                                    scope.launch {
                                        scrollState.animateScrollTo(0)
                                        delay(1000)
                                        showValidationNotification = false
                                    }
                                    return@Button
                                }
                                scope.launch {
                                    val categories = CategoryManager.getAll(db)
                                    val events = EventManager.getAll(db)
                                    val deadlines = DeadlineManager.getAll(db)
                                    val dependencyTasks = TaskManager.getAll(db).filter {
                                        it.status == 1 && it.startDate == null && it.startTime == null
                                    }

                                    val durationInMinutes = (taskDurationHours * 60) + taskDurationMinutes
                                    TaskManager.insert(
                                        db = db,
                                        title = taskTitle,
                                        notes = taskNotes.ifBlank { null },
                                        priority = taskPriority,
                                        breakable = taskIsBreakable,
                                        startDate = taskStartDate,
                                        startTime = taskStartTime,
                                        predictedDuration = durationInMinutes,
                                        categoryId = taskSelectedCategory?.let { categories.getOrNull(it)?.id },
                                        eventId = taskSelectedEvent?.let { events.getOrNull(it)?.id },
                                        deadlineId = taskSelectedDeadline?.let { deadlines.getOrNull(it)?.id },
                                        dependencyTaskId = taskSelectedDependencyTask?.let { dependencyTasks.getOrNull(it)?.id }
                                    )
                                    clearAllForms()
                                    showSuccessNotification = true
                                    delay(1000)
                                    showSuccessNotification = false
                                }
                            }
                            "Reminder" -> {
                                if (reminderTitle.isBlank()) {
                                    validationMessage = "Title is required"
                                    showValidationNotification = true
                                    scope.launch {
                                        scrollState.animateScrollTo(0)
                                        delay(1000)
                                        showValidationNotification = false
                                    }
                                    return@Button
                                }
                                scope.launch {
                                    val categories = CategoryManager.getAll(db)

                                    val recurRule = when (reminderRecurrenceFreq) {
                                        RecurrenceFrequency.NONE -> RecurrenceRule()
                                        RecurrenceFrequency.DAILY -> RecurrenceRule()
                                        RecurrenceFrequency.WEEKLY -> RecurrenceRule(daysOfWeek = reminderSelectedDaysOfWeek.toList())
                                        RecurrenceFrequency.MONTHLY -> RecurrenceRule(daysOfMonth = reminderSelectedDaysOfMonth.toList())
                                        RecurrenceFrequency.YEARLY -> RecurrenceRule(monthAndDay = Pair(reminderStartDate.dayOfMonth, reminderStartDate.monthValue))
                                    }

                                    ReminderManager.insert(
                                        db = db,
                                        title = reminderTitle,
                                        notes = reminderNotes.ifBlank { null },
                                        color = reminderColor,
                                        startDate = reminderStartDate,
                                        endDate = reminderEndDate,
                                        time = if (reminderIsAllDay) null else reminderTime,
                                        allDay = reminderIsAllDay,
                                        recurFreq = reminderRecurrenceFreq,
                                        recurRule = recurRule,
                                        categoryId = reminderSelectedCategory?.let { categories.getOrNull(it)?.id }
                                    )
                                    clearAllForms()
                                    showSuccessNotification = true
                                    delay(1000)
                                    showSuccessNotification = false
                                }
                            }
                            "Deadline" -> {
                                if (deadlineTitle.isBlank()) {
                                    validationMessage = "Title is required"
                                    showValidationNotification = true
                                    scope.launch {
                                        scrollState.animateScrollTo(0)
                                        delay(1000)
                                        showValidationNotification = false
                                    }
                                    return@Button
                                }
                                scope.launch {
                                    val categories = CategoryManager.getAll(db)
                                    val events = EventManager.getAll(db)

                                    // Insert deadline first
                                    DeadlineManager.insert(
                                        db = db,
                                        title = deadlineTitle,
                                        notes = deadlineNotes.ifBlank { null },
                                        date = deadlineDate,
                                        time = deadlineTime,
                                        categoryId = deadlineSelectedCategory?.let { categories.getOrNull(it)?.id },
                                        eventId = deadlineSelectedEvent?.let { events.getOrNull(it)?.id }
                                    )

                                    // If auto schedule task enabled, create task
                                    if (deadlineAutoScheduleTask) {
                                        val allDeadlines = DeadlineManager.getAll(db)
                                        val createdDeadline = allDeadlines.lastOrNull { deadline ->
                                            deadline.title == deadlineTitle &&
                                                    deadline.date == deadlineDate &&
                                                    deadline.time == deadlineTime
                                        }

                                        val durationInMinutes = (deadlineTaskDurationHours * 60) + deadlineTaskDurationMinutes
                                        TaskManager.insert(
                                            db = db,
                                            title = deadlineTitle,
                                            notes = deadlineNotes.ifBlank { null },
                                            priority = deadlineTaskPriority,
                                            breakable = deadlineTaskIsBreakable,
                                            startDate = null,
                                            startTime = null,
                                            predictedDuration = durationInMinutes,
                                            categoryId = deadlineSelectedCategory?.let { categories.getOrNull(it)?.id },
                                            eventId = deadlineSelectedEvent?.let { events.getOrNull(it)?.id },
                                            deadlineId = createdDeadline?.id,
                                            dependencyTaskId = null
                                        )
                                    }

                                    clearAllForms()
                                    showSuccessNotification = true
                                    delay(1000)
                                    showSuccessNotification = false
                                }
                            }
                            "Event" -> {
                                if (eventTitle.isBlank()) {
                                    validationMessage = "Title is required"
                                    showValidationNotification = true
                                    scope.launch {
                                        scrollState.animateScrollTo(0)
                                        delay(1000)
                                        showValidationNotification = false
                                    }
                                    return@Button
                                }

                                scope.launch {
                                    val recurRule = when (eventRecurrenceFreq) {
                                        RecurrenceFrequency.NONE -> RecurrenceRule()
                                        RecurrenceFrequency.DAILY -> RecurrenceRule()
                                        RecurrenceFrequency.WEEKLY -> RecurrenceRule(daysOfWeek = eventSelectedDaysOfWeek.toList())
                                        RecurrenceFrequency.MONTHLY -> RecurrenceRule(daysOfMonth = eventSelectedDaysOfMonth.toList())
                                        RecurrenceFrequency.YEARLY -> RecurrenceRule(monthAndDay = Pair(eventStartDate.dayOfMonth, eventStartDate.monthValue))
                                    }

                                    // Check for overlap with other Events
                                    val eventOverlapInfo = checkEventOverlapWithEvents(
                                        db = db,
                                        startDate = eventStartDate,
                                        endDate = eventEndDate,
                                        startTime = eventStartTime,
                                        endTime = eventEndTime,
                                        recurFreq = eventRecurrenceFreq,
                                        recurRule = recurRule
                                    )

                                    if (eventOverlapInfo.hasOverlap) {
                                        overlapMessage = formatOverlapMessage(eventOverlapInfo)
                                        showOverlapNotification = true
                                        scrollState.animateScrollTo(0)
                                        delay(2000)
                                        showOverlapNotification = false
                                        return@launch
                                    }

                                    // Check for overlap with Task Buckets
                                    val bucketOverlapInfo = checkEventOverlapWithBuckets(
                                        db = db,
                                        startDate = eventStartDate,
                                        endDate = eventEndDate,
                                        startTime = eventStartTime,
                                        endTime = eventEndTime,
                                        recurFreq = eventRecurrenceFreq,
                                        recurRule = recurRule
                                    )

                                    if (bucketOverlapInfo.hasOverlap) {
                                        overlapMessage = formatOverlapMessage(bucketOverlapInfo)
                                        showOverlapNotification = true
                                        scrollState.animateScrollTo(0)
                                        delay(2000)
                                        showOverlapNotification = false
                                        return@launch
                                    }

                                    // No overlap, proceed with save
                                    val categories = CategoryManager.getAll(db)

                                    EventManager.insert(
                                        db = db,
                                        title = eventTitle,
                                        notes = eventNotes.ifBlank { null },
                                        color = eventColor,
                                        startDate = eventStartDate,
                                        endDate = eventEndDate,
                                        startTime = eventStartTime,
                                        endTime = eventEndTime,
                                        recurFreq = eventRecurrenceFreq,
                                        recurRule = recurRule,
                                        categoryId = eventSelectedCategory?.let { categories.getOrNull(it)?.id }
                                    )
                                    clearAllForms()
                                    showSuccessNotification = true
                                    delay(1000)
                                    showSuccessNotification = false
                                }
                            }
                            "Task Bucket" -> {
                                scope.launch {
                                    val recurRule = when (bucketRecurrenceFreq) {
                                        RecurrenceFrequency.NONE -> RecurrenceRule()
                                        RecurrenceFrequency.DAILY -> RecurrenceRule()
                                        RecurrenceFrequency.WEEKLY -> RecurrenceRule(daysOfWeek = bucketSelectedDaysOfWeek.toList())
                                        RecurrenceFrequency.MONTHLY -> RecurrenceRule(daysOfMonth = bucketSelectedDaysOfMonth.toList())
                                        RecurrenceFrequency.YEARLY -> RecurrenceRule(monthAndDay = Pair(bucketStartDate.dayOfMonth, bucketStartDate.monthValue))
                                    }

                                    // Check for overlap with other Task Buckets
                                    val bucketOverlapInfo = checkBucketOverlapWithBuckets(
                                        db = db,
                                        startDate = bucketStartDate,
                                        endDate = bucketEndDate,
                                        startTime = bucketStartTime,
                                        endTime = bucketEndTime,
                                        recurFreq = bucketRecurrenceFreq,
                                        recurRule = recurRule
                                    )

                                    if (bucketOverlapInfo.hasOverlap) {
                                        overlapMessage = formatOverlapMessage(bucketOverlapInfo)
                                        showOverlapNotification = true
                                        scrollState.animateScrollTo(0)
                                        delay(2000)
                                        showOverlapNotification = false
                                        return@launch
                                    }

                                    // Check for overlap with Events
                                    val eventOverlapInfo = checkBucketOverlapWithEvents(
                                        db = db,
                                        startDate = bucketStartDate,
                                        endDate = bucketEndDate,
                                        startTime = bucketStartTime,
                                        endTime = bucketEndTime,
                                        recurFreq = bucketRecurrenceFreq,
                                        recurRule = recurRule
                                    )

                                    if (eventOverlapInfo.hasOverlap) {
                                        overlapMessage = formatOverlapMessage(eventOverlapInfo)
                                        showOverlapNotification = true
                                        scrollState.animateScrollTo(0)
                                        delay(2000)
                                        showOverlapNotification = false
                                        return@launch
                                    }

                                    // No overlap, proceed with save
                                    TaskBucketManager.insert(
                                        db = db,
                                        startDate = bucketStartDate,
                                        endDate = bucketEndDate,
                                        startTime = bucketStartTime,
                                        endTime = bucketEndTime,
                                        recurFreq = bucketRecurrenceFreq,
                                        recurRule = recurRule
                                    )
                                    clearAllForms()
                                    showSuccessNotification = true
                                    delay(1000)
                                    showSuccessNotification = false
                                }
                            }
                            "Category" -> {
                                if (categoryTitle.isBlank()) {
                                    validationMessage = "Title is required"
                                    showValidationNotification = true
                                    scope.launch {
                                        scrollState.animateScrollTo(0)
                                        delay(1000)
                                        showValidationNotification = false
                                    }
                                    return@Button
                                }
                                scope.launch {
                                    CategoryManager.insert(
                                        db = db,
                                        title = categoryTitle,
                                        notes = categoryNotes.ifBlank { null },
                                        color = categoryColor
                                    )
                                    clearAllForms()
                                    showSuccessNotification = true
                                    delay(1000)
                                    showSuccessNotification = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Save", fontSize = 16.sp)
                }
            }
        }

        // Validation notification
        AnimatedVisibility(
            visible = showValidationNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = PrimaryColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shadowElevation = 8.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        validationMessage,
                        color = BackgroundColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlap notification
        AnimatedVisibility(
            visible = showOverlapNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = PrimaryColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shadowElevation = 8.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        overlapMessage,
                        color = BackgroundColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Success notification
        AnimatedVisibility(
            visible = showSuccessNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = PrimaryColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shadowElevation = 8.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Saved",
                        color = BackgroundColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}