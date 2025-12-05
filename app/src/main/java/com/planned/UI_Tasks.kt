package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

// Priority colors for task display
val priorityColors = listOf(
    Preset31, // 1 - Red
    Preset32, // 2 - Orange
    Preset33, // 3 - Yellow
    Preset34, // 4 - Lime
    Preset35  // 5 - Green
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Tasks(db: AppDatabase) {
    var currentView by remember { mutableStateOf("main") } // main, unscheduled, scheduled, info, pomodoro
    var selectedTask by remember { mutableStateOf<MasterTask?>(null) }
    var selectedInterval by remember { mutableStateOf<TaskInterval?>(null) }

    when (currentView) {
        "main" -> TasksMainView(
            db = db,
            onUnscheduledClick = { currentView = "unscheduled" },
            onScheduledClick = { currentView = "scheduled" }
        )
        "unscheduled" -> UnscheduledTasksList(
            db = db,
            onBack = { currentView = "main" },
            onTaskClick = { task ->
                selectedTask = task
                currentView = "info"
            },
            onPlayClick = { task ->
                selectedTask = task
                currentView = "pomodoro"
            }
        )
        "scheduled" -> ScheduledTasksList(
            db = db,
            onBack = { currentView = "main" },
            onTaskClick = { interval, task ->
                selectedInterval = interval
                selectedTask = task
                currentView = "info"
            },
            onPlayClick = { interval, task ->
                selectedInterval = interval
                selectedTask = task
                currentView = "pomodoro"
            }
        )
        "info" -> selectedTask?.let { task ->
            TaskInfoPage(
                db = db,
                task = task,
                onBack = {
                    currentView = if (selectedInterval != null) "scheduled" else "unscheduled"
                    selectedTask = null
                    selectedInterval = null
                },
                onUpdate = { currentView = "update" }
            )
        }
        "update" -> selectedTask?.let { task ->
            TaskUpdateForm(
                db = db,
                task = task,
                onBack = { currentView = "info" }
            )
        }
        "pomodoro" -> selectedTask?.let { task ->
            PomodoroPage(
                db = db,
                task = task,
                onBack = {
                    currentView = if (selectedInterval != null) "scheduled" else "unscheduled"
                    selectedTask = null
                    selectedInterval = null
                }
            )
        }
    }
}

/* MAIN VIEW */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TasksMainView(
    db: AppDatabase,
    onUnscheduledClick: () -> Unit,
    onScheduledClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TaskCategoryBox(
                db = db,
                title = "Unscheduled \nTasks",
                modifier = Modifier.weight(1f),
                onClick = onUnscheduledClick
            )
            TaskCategoryBox(
                db = db,
                title = "Scheduled \nTasks",
                modifier = Modifier.weight(1f),
                onClick = onScheduledClick
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskCategoryBox(
    db: AppDatabase,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var taskCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val tasks = db.taskDao().getAllMasterTasks().filter { it.status != 3 }
        taskCount = if (title.contains("Unscheduled")) {
            tasks.filter { it.noIntervals == 0 }.size
        } else {
            tasks.filter { it.noIntervals > 0 }.size
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // Count indicator circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (taskCount > 0) PrimaryColor else Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = taskCount.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Title text
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
        }
    }
}

/* UNSCHEDULED TASKS LIST */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UnscheduledTasksList(
    db: AppDatabase,
    onBack: () -> Unit,
    onTaskClick: (MasterTask) -> Unit,
    onPlayClick: (MasterTask) -> Unit
) {
    var tasks by remember { mutableStateOf<List<MasterTask>>(emptyList()) }

    LaunchedEffect(Unit) {
        tasks = db.taskDao().getAllMasterTasks()
            .filter { it.noIntervals == 0 && it.status != 3 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Back",
                fontSize = 16.sp,
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                UnscheduledTaskItem(
                    db = db,
                    task = task,
                    onClick = { onTaskClick(task) },
                    onPlayClick = { onPlayClick(task) }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UnscheduledTaskItem(
    db: AppDatabase,
    task: MasterTask,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    var category by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(task.categoryId) {
        category = task.categoryId?.let { db.categoryDao().getById(it) }
    }

    val innerColor = category?.let { Converters.toColor(it.color) } ?: Color.Gray
    val outerColor = priorityColors.getOrNull(task.priority - 1) ?: Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bullseye circle
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(outerColor),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(innerColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title
        Text(
            text = task.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Play button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryColor)
                .clickable { onPlayClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Start",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* SCHEDULED TASKS LIST */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduledTasksList(
    db: AppDatabase,
    onBack: () -> Unit,
    onTaskClick: (TaskInterval, MasterTask) -> Unit,
    onPlayClick: (TaskInterval, MasterTask) -> Unit
) {
    var intervals by remember { mutableStateOf<List<TaskInterval>>(emptyList()) }
    var masterTasks by remember { mutableStateOf<Map<Int, MasterTask>>(emptyMap()) }

    LaunchedEffect(Unit) {
        intervals = db.taskDao().getAllIntervals()
        val tasks = db.taskDao().getAllMasterTasks()
        masterTasks = tasks.associateBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Back",
                fontSize = 16.sp,
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group intervals by masterTaskId
            val groupedIntervals = intervals.groupBy { it.masterTaskId }

            groupedIntervals.forEach { (masterTaskId, taskIntervals) ->
                item {
                    masterTasks[masterTaskId]?.let { masterTask ->
                        ScheduledTaskItem(
                            db = db,
                            masterTask = masterTask,
                            intervals = taskIntervals,
                            onClick = { onTaskClick(taskIntervals.first(), masterTask) },
                            onPlayClick = { onPlayClick(taskIntervals.first(), masterTask) }
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduledTaskItem(
    db: AppDatabase,
    masterTask: MasterTask,
    intervals: List<TaskInterval>,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    var category by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(masterTask.categoryId) {
        category = masterTask.categoryId?.let { db.categoryDao().getById(it) }
    }

    val innerColor = category?.let { Converters.toColor(it.color) } ?: Color.Gray
    val outerColor = priorityColors.getOrNull(masterTask.priority - 1) ?: Color.Gray

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bullseye circle
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(outerColor),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(innerColor)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title with ellipsis
            Text(
                text = masterTask.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Play button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor)
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Start",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Dates and times
        Column(
            modifier = Modifier.padding(start = 42.dp)
        ) {
            intervals.sortedBy { it.intervalNo }.forEach { interval ->
                Text(
                    text = "${interval.occurDate.format(dateFormatter)}  ${interval.startTime.format(timeFormatter)} - ${interval.endTime.format(timeFormatter)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/* TASK INFO PAGE */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskInfoPage(
    db: AppDatabase,
    task: MasterTask,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf<Category?>(null) }
    var event by remember { mutableStateOf<MasterEvent?>(null) }
    var deadline by remember { mutableStateOf<Deadline?>(null) }
    var intervals by remember { mutableStateOf<List<TaskInterval>>(emptyList()) }

    LaunchedEffect(task) {
        category = task.categoryId?.let { db.categoryDao().getById(it) }
        event = task.eventId?.let { db.eventDao().getMasterEventById(it) }
        deadline = task.deadlineId?.let { db.deadlineDao().getById(it) }
        intervals = db.taskDao().getIntervalsForTask(task.id).sortedBy { it.intervalNo }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Back",
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info fields
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoField("Title", task.title)
            }
            item {
                InfoField("Notes", task.notes ?: "None")
            }
            item {
                InfoField("Priority", task.priority.toString())
            }
            item {
                InfoField("Breakable", if (task.breakable == true) "Yes" else "No")
            }
            item {
                InfoField("Duration", "${task.predictedDuration} minutes")
            }
            item {
                val scheduleText = if (intervals.isEmpty()) {
                    "None"
                } else {
                    intervals.joinToString("\n") { interval ->
                        "${interval.occurDate.format(dateFormatter)} ${interval.startTime.format(timeFormatter)} - ${interval.endTime.format(timeFormatter)}"
                    }
                }
                InfoField("Schedule", scheduleText)
            }
            item {
                InfoField("Deadline", deadline?.title ?: "None")
            }
            item {
                InfoField("Event", event?.title ?: "None")
            }
            item {
                InfoField("Category", category?.title ?: "None")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        TaskManager.delete(db, task.id)
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Delete", fontSize = 16.sp, color = Color.White)
            }
            Button(
                onClick = onUpdate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Update", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun InfoField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp)
    }
}

/* TASK UPDATE FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskUpdateForm(
    db: AppDatabase,
    task: MasterTask,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State variables matching TaskForm
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes ?: "") }
    var priority by remember { mutableIntStateOf(task.priority) }
    var isBreakable by remember { mutableStateOf(task.breakable ?: false) }
    var isAutoSchedule by remember { mutableStateOf(task.startDate == null || task.startTime == null) }
    var startDate by remember { mutableStateOf(task.startDate) }
    var startTime by remember { mutableStateOf(task.startTime) }
    var durationHours by remember { mutableIntStateOf(task.predictedDuration / 60) }
    var durationMinutes by remember { mutableIntStateOf(task.predictedDuration % 60) }
    var breakableLockedByDuration by remember { mutableStateOf(false) }

    // Categories, events, deadlines - converted to indices
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var events by remember { mutableStateOf<List<MasterEvent>>(emptyList()) }
    var deadlines by remember { mutableStateOf<List<Deadline>>(emptyList()) }

    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var selectedEvent by remember { mutableStateOf<Int?>(null) }
    var selectedDeadline by remember { mutableStateOf<Int?>(null) }

    var resetTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
        events = EventManager.getAll(db)
        deadlines = DeadlineManager.getAll(db)

        // Convert IDs to indices
        selectedCategory = task.categoryId?.let { catId ->
            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
        }
        selectedEvent = task.eventId?.let { evId ->
            events.indexOfFirst { it.id == evId }.takeIf { it >= 0 }
        }
        selectedDeadline = task.deadlineId?.let { dlId ->
            deadlines.indexOfFirst { it.id == dlId }.takeIf { it >= 0 }
        }
    }

    fun clearForm() {
        title = task.title
        notes = task.notes ?: ""
        priority = task.priority
        isBreakable = task.breakable ?: false
        isAutoSchedule = task.startDate == null || task.startTime == null
        startDate = task.startDate
        startTime = task.startTime
        durationHours = task.predictedDuration / 60
        durationMinutes = task.predictedDuration % 60

        selectedCategory = task.categoryId?.let { catId ->
            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
        }
        selectedEvent = task.eventId?.let { evId ->
            events.indexOfFirst { it.id == evId }.takeIf { it >= 0 }
        }
        selectedDeadline = task.deadlineId?.let { dlId ->
            deadlines.indexOfFirst { it.id == dlId }.takeIf { it >= 0 }
        }

        resetTrigger++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Back",
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Use TaskForm with current values
        TaskForm(
            db = db,
            title = title,
            onTitleChange = { title = it },
            notes = notes,
            onNotesChange = { notes = it },
            priority = priority,
            onPriorityChange = { priority = it },
            isBreakable = isBreakable,
            onBreakableChange = { isBreakable = it },
            isAutoSchedule = isAutoSchedule,
            startDate = startDate,
            startTime = startTime,
            onScheduleChange = { auto, date, time ->
                isAutoSchedule = auto
                startDate = date
                startTime = time
            },
            durationHours = durationHours,
            durationMinutes = durationMinutes,
            onDurationChange = { h, m ->
                durationHours = h
                durationMinutes = m
            },
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it },
            selectedEvent = selectedEvent,
            onEventChange = { selectedEvent = it },
            selectedDeadline = selectedDeadline,
            onDeadlineChange = { selectedDeadline = it },
            breakableLockedByDuration = breakableLockedByDuration,
            onBreakableLockedByDurationChange = { breakableLockedByDuration = it },
            resetTrigger = resetTrigger
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Clear and Save buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { clearForm() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Clear", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (title.isBlank()) {
                        return@Button
                    }
                    scope.launch {
                        val durationInMinutes = (durationHours * 60) + durationMinutes
                        val updatedTask = task.copy(
                            title = title,
                            notes = notes.ifBlank { null },
                            priority = priority,
                            breakable = isBreakable,
                            predictedDuration = durationInMinutes,
                            startDate = if (isAutoSchedule) null else startDate,
                            startTime = if (isAutoSchedule) null else startTime,
                            categoryId = selectedCategory?.let { categories.getOrNull(it)?.id },
                            eventId = selectedEvent?.let { events.getOrNull(it)?.id },
                            deadlineId = selectedDeadline?.let { deadlines.getOrNull(it)?.id }
                        )
                        TaskManager.update(db, updatedTask)
                        onBack()
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
}

/* POMODORO PAGE */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PomodoroPage(
    db: AppDatabase,
    task: MasterTask,
    onBack: () -> Unit
) {
    var elapsedMinutes by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var intervals by remember { mutableStateOf<List<TaskInterval>>(emptyList()) }
    var currentTask by remember { mutableStateOf(task) }

    LaunchedEffect(Unit) {
        intervals = db.taskDao().getIntervalsForTask(task.id).sortedBy { it.intervalNo }
        currentTask = db.taskDao().getMasterTaskById(task.id) ?: task
    }

    // Timer effect
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(60000L) // 1 minute
            elapsedMinutes++
        }
    }

    // Calculate current interval and stats
    val totalActualDuration = (currentTask.actualDuration ?: 0) + elapsedMinutes
    val currentIntervalData = getCurrentIntervalData(intervals, totalActualDuration, currentTask)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryColor)
                    .clickable { onBack() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Back",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Notes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(text = currentTask.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }

        if (!currentTask.notes.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(CardColor), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(text = currentTask.notes!!, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Timer display
        PomodoroTimer(elapsedMinutes)

        Spacer(modifier = Modifier.height(18.dp))

        // Stats
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Interval: ${currentIntervalData.currentIntervalNo}/${currentTask.noIntervals}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Time Left: ${formatDuration(currentIntervalData.timeLeft)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Overtime: ${formatDuration(currentIntervalData.overtime)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (currentIntervalData.overtime > 0) Color.Red else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        if (isRunning) {
                            // Stop - update database
                            isRunning = false
                            updateTaskProgress(db, currentTask, intervals, elapsedMinutes)
                            elapsedMinutes = 0
                            // Reload task and intervals
                            currentTask = db.taskDao().getMasterTaskById(task.id) ?: task
                            intervals = db.taskDao().getIntervalsForTask(task.id).sortedBy { it.intervalNo }
                        } else {
                            // Start
                            if (currentTask.status == 1) {
                                // First time starting - update interval status to 2
                                intervals.firstOrNull()?.let { firstInterval ->
                                    db.taskDao().updateInterval(firstInterval.copy(status = 2))
                                }
                                db.taskDao().update(currentTask.copy(status = 2))
                                currentTask = currentTask.copy(status = 2)
                            }
                            isRunning = true
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                ),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(if (isRunning) "Stop" else "Start", fontSize = 16.sp, color = Color.White)
            }

            Button(
                onClick = {
                    scope.launch {
                        // Complete task
                        if (isRunning) {
                            updateTaskProgress(db, currentTask, intervals, elapsedMinutes)
                        }

                        // Delete all intervals
                        intervals.forEach { interval ->
                            db.taskDao().deleteInterval(interval.id)
                        }

                        // Update master task
                        db.taskDao().update(
                            currentTask.copy(
                                status = 3,
                                noIntervals = 0
                            )
                        )

                        onBack()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Complete", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PomodoroTimer(elapsedMinutes: Int) {
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    val progress = (minutes / 60f)

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer circle (filling clockwise)
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawArc(
                color = PrimaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx())
            )
        }

        // Inner circle with time
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.US, "%02d:%02d", hours, minutes),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* HELPER FUNCTIONS */
data class IntervalData(
    val currentIntervalNo: Int,
    val timeLeft: Int,
    val overtime: Int
)

@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentIntervalData(
    intervals: List<TaskInterval>,
    totalActualDuration: Int,
    task: MasterTask
): IntervalData {
    if (intervals.isEmpty()) {
        return IntervalData(0, 0, 0)
    }

    var cumulativeDuration = 0
    var currentInterval = intervals.first()

    for (interval in intervals) {
        val intervalDuration = calculateIntervalDuration(interval)
        if (totalActualDuration <= cumulativeDuration + intervalDuration) {
            currentInterval = interval
            break
        }
        cumulativeDuration += intervalDuration
    }

    val intervalDuration = calculateIntervalDuration(currentInterval)
    val durationIntoInterval = totalActualDuration - cumulativeDuration
    val timeLeft = maxOf(0, intervalDuration - durationIntoInterval)

    // Calculate overtime (only if on last interval and exceeded predicted duration)
    val overtime = if (currentInterval.intervalNo == task.noIntervals && task.predictedDuration < totalActualDuration) {
        totalActualDuration - task.predictedDuration
    } else {
        0
    }

    return IntervalData(
        currentIntervalNo = currentInterval.intervalNo,
        timeLeft = timeLeft,
        overtime = overtime
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun calculateIntervalDuration(interval: TaskInterval): Int {
    val startMinutes = interval.startTime.hour * 60 + interval.startTime.minute
    val endMinutes = interval.endTime.hour * 60 + interval.endTime.minute
    return endMinutes - startMinutes
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun updateTaskProgress(
    db: AppDatabase,
    task: MasterTask,
    intervals: List<TaskInterval>,
    elapsedMinutes: Int
) {
    val newActualDuration = (task.actualDuration ?: 0) + elapsedMinutes
    val newTimeLeft = maxOf(0, task.predictedDuration - newActualDuration)
    val overtime = if (newActualDuration > task.predictedDuration) {
        newActualDuration - task.predictedDuration
    } else {
        0
    }

    // Update master task
    db.taskDao().update(
        task.copy(
            actualDuration = newActualDuration,
            timeLeft = newTimeLeft,
            overTime = overtime
        )
    )

    // Update intervals and delete completed ones
    var cumulativeDuration = 0
    for (interval in intervals.sortedBy { it.intervalNo }) {
        val intervalDuration = calculateIntervalDuration(interval)

        if (newActualDuration >= cumulativeDuration + intervalDuration) {
            // This interval is complete
            if (interval.intervalNo < task.noIntervals) {
                // Not the last interval - delete it
                db.taskDao().deleteInterval(interval.id)
            } else {
                // Last interval - update with overtime
                val intervalTimeLeft = 0
                val intervalOvertime = newActualDuration - cumulativeDuration - intervalDuration
                db.taskDao().updateInterval(
                    interval.copy(
                        status = 2,
                        timeLeft = intervalTimeLeft,
                        overTime = intervalOvertime
                    )
                )
            }
        } else {
            // This interval is current or future
            val durationIntoInterval = newActualDuration - cumulativeDuration
            val intervalTimeLeft = maxOf(0, intervalDuration - durationIntoInterval)
            db.taskDao().updateInterval(
                interval.copy(
                    status = 2,
                    timeLeft = intervalTimeLeft,
                    overTime = 0
                )
            )
        }

        cumulativeDuration += intervalDuration
    }
}

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format(Locale.US, "%02d:%02d", hours, mins)
}