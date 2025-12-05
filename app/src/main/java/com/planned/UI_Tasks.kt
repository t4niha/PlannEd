package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

/* MAIN VIEW - Two boxes for Unscheduled and Scheduled */
@Composable
fun TasksMainView(
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
                title = "Unscheduled Tasks",
                modifier = Modifier.weight(1f),
                onClick = onUnscheduledClick
            )
            TaskCategoryBox(
                title = "Scheduled Tasks",
                modifier = Modifier.weight(1f),
                onClick = onScheduledClick
            )
        }
    }
}

@Composable
fun TaskCategoryBox(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray)
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
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
        TextButton(onClick = onBack) {
            Text("← Back", fontSize = 16.sp)
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
                .size(40.dp)
                .clip(CircleShape)
                .background(outerColor),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
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
        TextButton(onClick = onBack) {
            Text("← Back", fontSize = 16.sp)
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
                .size(40.dp)
                .clip(CircleShape)
                .background(outerColor),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(innerColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and timings
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = masterTask.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Show all timings for breakable tasks
            intervals.sortedBy { it.intervalNo }.forEach { interval ->
                Text(
                    text = "${interval.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${interval.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

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

    LaunchedEffect(task) {
        category = task.categoryId?.let { db.categoryDao().getById(it) }
        event = task.eventId?.let { db.eventDao().getMasterEventById(it) }
        deadline = task.deadlineId?.let { db.deadlineDao().getById(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        // Back button
        TextButton(onClick = onBack) {
            Text("← Back", fontSize = 16.sp)
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
                InfoField("Predicted Duration", "${task.predictedDuration} minutes")
            }
            item {
                InfoField("Actual Duration", "${task.actualDuration ?: 0} minutes")
            }
            item {
                InfoField("Status", when(task.status) {
                    1 -> "Not Started"
                    2 -> "In Progress"
                    3 -> "Completed"
                    else -> "Unknown"
                })
            }
            item {
                InfoField("Category", category?.title ?: "None")
            }
            item {
                InfoField("Event", event?.title ?: "None")
            }
            item {
                InfoField("Deadline", deadline?.title ?: "None")
            }
            if (task.startDate != null && task.startTime != null) {
                item {
                    InfoField("Start Date", task.startDate.toString())
                }
                item {
                    InfoField("Start Time", task.startTime.toString())
                }
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete", color = Color.White)
            }
            Button(
                onClick = onUpdate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Update", color = Color.White)
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
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
    // Use the same creation form but with pre-filled values
    // This will be the TaskForm from UI_CreationForms.kt with initial values
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes ?: "") }
    var priority by remember { mutableIntStateOf(task.priority) }
    var breakable by remember { mutableStateOf(task.breakable) }
    var predictedDuration by remember { mutableIntStateOf(task.predictedDuration) }
    var categoryId by remember { mutableStateOf(task.categoryId) }
    var eventId by remember { mutableStateOf(task.eventId) }
    var deadlineId by remember { mutableStateOf(task.deadlineId) }
    var startDate by remember { mutableStateOf(task.startDate) }
    var startTime by remember { mutableStateOf(task.startTime) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        // Back button
        TextButton(onClick = onBack) {
            Text("← Back", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Update Task", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // Form fields (simplified - you can expand this with proper form fields)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Add more fields as needed
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save button
        Button(
            onClick = {
                scope.launch {
                    val updatedTask = task.copy(
                        title = title,
                        notes = notes.ifBlank { null },
                        priority = priority,
                        breakable = breakable,
                        predictedDuration = predictedDuration,
                        categoryId = categoryId,
                        eventId = eventId,
                        deadlineId = deadlineId,
                        startDate = startDate,
                        startTime = startTime
                    )
                    TaskManager.update(db, updatedTask)
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text("Save", color = Color.White)
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
            TextButton(onClick = onBack) {
                Text("← Back", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Notes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(text = currentTask.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!currentTask.notes.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(CardColor), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(text = currentTask.notes!!, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Timer display
        PomodoroTimer(elapsedMinutes)

        Spacer(modifier = Modifier.height(24.dp))

        // Stats
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
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
            if (currentIntervalData.overtime > 0) {
                Text(
                    text = "Overtime: ${formatDuration(currentIntervalData.overtime)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Red
                )
            }
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
                    containerColor = if (isRunning) Color.Red else PrimaryColor
                )
            ) {
                Text(if (isRunning) "Stop" else "Start", color = Color.White)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Complete", color = Color.White)
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
        // Outer circle (depleting)
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawArc(
                color = PrimaryColor,
                startAngle = -90f,
                sweepAngle = 360f * (1f - progress),
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