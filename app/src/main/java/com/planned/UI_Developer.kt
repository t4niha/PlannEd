package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.withSave
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

// Occurrence generation months
var generationMonths by mutableIntStateOf(1)

/* DATABASE PREVIEW */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Developer(db: AppDatabase) {
    val scrollStateVertical = rememberScrollState()
    val scrollStateHorizontal = rememberScrollState()

    // DB
    val context = LocalContext.current
    val db = AppDatabaseProvider.getDatabase(context)

    // State holders
    var categories by remember { mutableStateOf(listOf<Category>()) }
    var masterEvents by remember { mutableStateOf(listOf<MasterEvent>()) }
    var eventOccurrences by remember { mutableStateOf(listOf<EventOccurrence>()) }
    var deadlines by remember { mutableStateOf(listOf<Deadline>()) }
    var masterBuckets by remember { mutableStateOf(listOf<MasterTaskBucket>()) }
    var bucketOccurrences by remember { mutableStateOf(listOf<TaskBucketOccurrence>()) }
    var masterTasks by remember { mutableStateOf(listOf<MasterTask>()) }
    var taskIntervals by remember { mutableStateOf(listOf<TaskInterval>()) }
    var masterReminders by remember { mutableStateOf(listOf<MasterReminder>()) }
    var reminderOccurrences by remember { mutableStateOf(listOf<ReminderOccurrence>()) }
    var settings by remember { mutableStateOf(listOf<AppSetting>()) }
    var categoryATIList by remember { mutableStateOf(listOf<CategoryATI>()) }
    var eventATIList by remember { mutableStateOf(listOf<EventATI>()) }

    // Load DB
    fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            categories = db.categoryDao().getAll()
            masterEvents = db.eventDao().getAllMasterEvents()
            eventOccurrences = db.eventDao().getAllOccurrences()
            deadlines = db.deadlineDao().getAll()
            masterBuckets = db.taskBucketDao().getAllMasterBuckets()
            bucketOccurrences = db.taskBucketDao().getAllBucketOccurrences()
            masterTasks = db.taskDao().getAllMasterTasks()
            taskIntervals = db.taskDao().getAllIntervals()
            masterReminders = db.reminderDao().getAllMasterReminders()
            reminderOccurrences = db.reminderDao().getAllOccurrences()
            db.settingsDao().getAll()?.let { settings = listOf(it) } ?: run { settings = emptyList() }
            categoryATIList = db.categoryATIDao().getAll()
            eventATIList = db.eventATIDao().getAll()
        }
    }
    LaunchedEffect(Unit) { refreshData() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(12.dp)
    ) {
        // Controls
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(CardColor), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Generation Months adjuster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Generate Months:",
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Decrease button
                    Button(
                        onClick = {
                            if (generationMonths > 1) generationMonths--
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor,
                            contentColor = BackgroundColor
                        ),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Display current value
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .background(BackgroundColor, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            generationMonths.toString(),
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Increase button
                    Button(
                        onClick = {
                            if (generationMonths < 24) generationMonths++
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor,
                            contentColor = BackgroundColor
                        ),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", fontSize = 20.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Clear and Sample buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            db.reminderDao().getAllMasterReminders()
                                .forEach { db.reminderDao().deleteMasterReminder(it.id) }
                            db.taskDao().getAllMasterTasks()
                                .forEach { db.taskDao().deleteMasterTask(it.id) }
                            db.taskBucketDao().getAllMasterBuckets()
                                .forEach { db.taskBucketDao().deleteMasterBucket(it.id) }
                            db.deadlineDao().getAll()
                                .forEach { db.deadlineDao().deleteById(it.id) }
                            db.eventDao().getAllMasterEvents()
                                .forEach { db.eventDao().deleteMasterEvent(it.id) }
                            db.categoryDao().getAll()
                                .forEach { db.categoryDao().deleteById(it.id) }
                            db.categoryATIDao().getAll()
                                .forEach { db.categoryATIDao().deleteById(it.categoryId) }
                            db.eventATIDao().getAll()
                                .forEach { db.eventATIDao().deleteById(it.eventId) }
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Clear", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            runSample(db)
                            trimAndExtendOccurrences(db)
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Sample", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollStateVertical)
                .horizontalScroll(scrollStateHorizontal)
                .padding(8.dp)
        ) {
            // ATI Scatter Plot
            ATIScatterPlot(
                db = db,
                categories = categories,
                masterEvents = masterEvents
            )

            // Database table
            @Composable
            fun <T> Table(
                title: String,
                data: List<T>,
                headers: List<String>,
                rowContent: (T) -> List<String>
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.border(1.dp, GRID_COLOR)) {
                    // Header
                    Row(
                        modifier = Modifier.background(HEADER_BG).fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        headers.forEachIndexed { index, header ->
                            Box(
                                modifier = Modifier
                                    .width(COLUMN_WIDTH)
                                    .fillMaxHeight()
                                    .drawWithContent {
                                        drawContent()
                                        if (index < headers.lastIndex) drawLine(
                                            color = GRID_COLOR,
                                            start = Offset(size.width, 0f),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                    .padding(8.dp)
                            ) { Text(header, color = Color.Black) }
                        }
                    }

                    // Rows
                    val rows = data.ifEmpty { listOf(null) }
                    rows.forEach { row ->
                        val values = row?.let { rowContent(it) } ?: headers.map { "" }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .drawWithContent {
                                    drawContent()
                                    drawLine(
                                        color = GRID_COLOR,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                        ) {
                            values.forEachIndexed { index, value ->
                                Box(
                                    modifier = Modifier
                                        .width(COLUMN_WIDTH)
                                        .fillMaxHeight()
                                        .drawWithContent {
                                            drawContent()
                                            if (index < values.lastIndex) drawLine(
                                                color = GRID_COLOR,
                                                start = Offset(size.width, 0f),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                        .padding(8.dp)
                                ) { Text(value, color = Color.Black) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            fun formatRecurrenceRule(rule: RecurrenceRule, freq: RecurrenceFrequency): String {
                return when (freq) {
                    RecurrenceFrequency.NONE, RecurrenceFrequency.DAILY -> ""
                    RecurrenceFrequency.WEEKLY -> rule.daysOfWeek?.joinToString(", ") ?: ""
                    RecurrenceFrequency.MONTHLY -> rule.daysOfMonth?.joinToString(", ") ?: ""
                    RecurrenceFrequency.YEARLY -> rule.monthAndDay?.let { "${it.first}/${it.second}" }
                        ?: ""
                }
            }

            // Tables
            Table(
                title = "Categories",
                data = categories,
                headers = listOf("ID", "Title", "Notes", "Color")
            ) { c ->
                listOf(
                    c.id.toString(),
                    c.title,
                    c.notes ?: "",
                    c.color
                )
            }

            Table(
                title = "Master Events",
                data = masterEvents,
                headers = listOf(
                    "ID",
                    "Title",
                    "Notes",
                    "Color",
                    "Start Date",
                    "End Date",
                    "Start Time",
                    "End Time",
                    "Frequency",
                    "Rule",
                    "Category ID"
                )
            ) { e ->
                listOf(
                    e.id.toString(),
                    e.title,
                    e.notes ?: "",
                    e.color ?: "",
                    e.startDate.toString(),
                    e.endDate?.toString() ?: "",
                    e.startTime.toString(),
                    e.endTime.toString(),
                    e.recurFreq.name,
                    formatRecurrenceRule(e.recurRule, e.recurFreq),
                    e.categoryId?.toString() ?: ""
                )
            }

            Table(
                title = "Event Occurrences",
                data = eventOccurrences,
                headers = listOf(
                    "ID",
                    "Master Event ID",
                    "Notes",
                    "Occur Date",
                    "Start Time",
                    "End Time"
                )
            ) { o ->
                listOf(
                    o.id.toString(),
                    o.masterEventId.toString(),
                    o.notes ?: "",
                    o.occurDate.toString(),
                    o.startTime.toString(),
                    o.endTime.toString()
                )
            }

            Table(
                title = "Deadlines",
                data = deadlines,
                headers = listOf("ID", "Title", "Notes", "Date", "Time", "Category ID", "Event ID")
            ) { d ->
                listOf(
                    d.id.toString(),
                    d.title,
                    d.notes ?: "",
                    d.date.toString(),
                    d.time.toString(),
                    d.categoryId?.toString() ?: "",
                    d.eventId?.toString() ?: ""
                )
            }

            Table(
                title = "Master Task Buckets",
                data = masterBuckets,
                headers = listOf(
                    "ID",
                    "Start Date",
                    "End Date",
                    "Start Time",
                    "End Time",
                    "Frequency",
                    "Rule"
                )
            ) { b ->
                listOf(
                    b.id.toString(),
                    b.startDate.toString(),
                    b.endDate?.toString() ?: "",
                    b.startTime.toString(),
                    b.endTime.toString(),
                    b.recurFreq.name,
                    formatRecurrenceRule(b.recurRule, b.recurFreq)
                )
            }

            Table(
                title = "Task Bucket Occurrences",
                data = bucketOccurrences,
                headers = listOf(
                    "ID",
                    "Master Bucket ID",
                    "Occur Date",
                    "Start Time",
                    "End Time",
                    "Is Exception"
                )
            ) { o ->
                listOf(
                    o.id.toString(),
                    o.masterBucketId.toString(),
                    o.occurDate.toString(),
                    o.startTime.toString(),
                    o.endTime.toString(),
                    o.isException.toString()
                )
            }

            Table(
                title = "Master Tasks",
                data = masterTasks,
                headers = listOf(
                    "ID",
                    "Title",
                    "Notes",
                    "All Day",
                    "Breakable",
                    "No. Intervals",
                    "Start Date",
                    "Start Time",
                    "Duration",
                    "Actual Duration",
                    "Status",
                    "Time Left",
                    "Overtime",
                    "Category ID",
                    "Event ID",
                    "Deadline ID",
                    "Dependency Task ID"
                )
            ) { t ->
                listOf(
                    t.id.toString(),
                    t.title,
                    t.notes ?: "",
                    t.allDay?.toString() ?: "",
                    t.breakable?.toString() ?: "",
                    t.noIntervals.toString(),
                    t.startDate?.toString() ?: "",
                    t.startTime?.toString() ?: "",
                    t.predictedDuration.toString(),
                    t.actualDuration?.toString() ?: "",
                    t.status?.toString() ?: "",
                    t.timeLeft?.toString() ?: "",
                    t.overTime?.toString() ?: "",
                    t.categoryId?.toString() ?: "",
                    t.eventId?.toString() ?: "",
                    t.deadlineId?.toString() ?: "",
                    t.dependencyTaskId?.toString() ?: ""
                )
            }

            Table(
                title = "Task Intervals",
                data = taskIntervals,
                headers = listOf(
                    "ID",
                    "Master Task ID",
                    "Interval No.",
                    "Notes",
                    "Occur Date",
                    "Start Time",
                    "End Time",
                    "Status",
                    "Time Left",
                    "Overtime",
                    "ATI Padding"
                )
            ) { i ->
                listOf(
                    i.id.toString(),
                    i.masterTaskId.toString(),
                    i.intervalNo.toString(),
                    i.notes ?: "",
                    i.occurDate.toString(),
                    i.startTime.toString(),
                    i.endTime.toString(),
                    i.status?.toString() ?: "",
                    i.timeLeft?.toString() ?: "",
                    i.overTime?.toString() ?: "",
                    i.atiPadding.toString()
                )
            }

            Table(
                title = "Master Reminders",
                data = masterReminders,
                headers = listOf(
                    "ID",
                    "Title",
                    "Notes",
                    "Start Date",
                    "End Date",
                    "Time",
                    "All Day",
                    "Frequency",
                    "Rule",
                    "Category ID"
                )
            ) { r ->
                listOf(
                    r.id.toString(),
                    r.title,
                    r.notes ?: "",
                    r.startDate.toString(),
                    r.endDate?.toString() ?: "",
                    r.time?.toString() ?: "",
                    r.allDay.toString(),
                    r.recurFreq.name,
                    formatRecurrenceRule(r.recurRule, r.recurFreq),
                    r.categoryId?.toString() ?: ""
                )
            }

            Table(
                title = "Reminder Occurrences",
                data = reminderOccurrences,
                headers = listOf(
                    "ID",
                    "Master Reminder ID",
                    "Notes",
                    "Occur Date",
                    "Time",
                    "All Day"
                )
            ) { o ->
                listOf(
                    o.id.toString(),
                    o.masterReminderId.toString(),
                    o.notes ?: "",
                    o.occurDate.toString(),
                    o.time?.toString() ?: "",
                    o.allDay.toString()
                )
            }

            Table(
                title = "Category ATI",
                data = categoryATIList,
                headers = listOf(
                    "Category ID",
                    "Score",
                    "Deadline Misses",
                    "Avg Overtime",
                    "Tasks Completed",
                    "Predicted Padding"
                )
            ) { a ->
                listOf(
                    a.categoryId.toString(),
                    "%.3f".format(a.score),
                    a.deadlineMissCount.toString(),
                    "%.1f".format(a.avgOvertime),
                    a.tasksCompleted.toString(),
                    a.predictedPadding.toString()
                )
            }

            Table(
                title = "Event ATI",
                data = eventATIList,
                headers = listOf(
                    "Event ID",
                    "Score",
                    "Deadline Misses",
                    "Avg Overtime",
                    "Tasks Completed",
                    "Predicted Padding"
                )
            ) { a ->
                listOf(
                    a.eventId.toString(),
                    "%.3f".format(a.score),
                    a.deadlineMissCount.toString(),
                    "%.1f".format(a.avgOvertime),
                    a.tasksCompleted.toString(),
                    a.predictedPadding.toString()
                )
            }

            Table(
                title = "Settings",
                data = settings,
                headers = listOf("ID", "Start Week On Monday", "Primary Color", "Show Developer", "Break Duration", "Break Every")
            ) { s ->
                listOf(
                    s.id.toString(),
                    s.startWeekOnMonday.toString(),
                    s.primaryColor,
                    s.showDeveloper.toString(),
                    s.breakDuration.toString(),
                    s.breakEvery.toString()
                )
            }
        }
    }
}

/* REGRESSION MODEL */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ATIScatterPlot(
    db: AppDatabase,
    categories: List<Category>,
    masterEvents: List<MasterEvent>
) {
    val scope = rememberCoroutineScope()

    // Entity type selection
    var selectedType by remember { mutableStateOf("Category") }
    val types = listOf("Category", "Event")

    // Selected entity
    var selectedEntityIndex by remember { mutableIntStateOf(0) }

    // Data points: list of (predictedDuration, overTime)
    var dataPoints by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var atiRecord by remember { mutableStateOf<String>("") }
    var slope by remember { mutableFloatStateOf(0f) }
    var intercept by remember { mutableFloatStateOf(0f) }

    // Entity names for dropdown
    val entityNames = if (selectedType == "Category")
        categories.map { it.title }
    else
        masterEvents.map { it.title }

    // Load data when selection changes
    fun loadData() {
        scope.launch(Dispatchers.IO) {
            val allTasks = db.taskDao().getAllMasterTasks()
            val points = mutableListOf<Pair<Float, Float>>()

            if (selectedType == "Category" && categories.isNotEmpty()) {
                val cat = categories.getOrNull(selectedEntityIndex) ?: return@launch
                val ati = db.categoryATIDao().getById(cat.id)
                val completed = allTasks
                    .filter { it.categoryId == cat.id && it.status == 3 && it.allDay == null }
                    .takeLast(10)
                completed.forEach { t ->
                    points.add(Pair(t.predictedDuration.toFloat(), (t.overTime ?: 0).toFloat()))
                }
                atiRecord = "Score: ${"%.3f".format(ati?.score ?: 0f)},  " +
                        "Misses: ${ati?.deadlineMissCount ?: 0},  " +
                        "Avg OT: ${"%.1f".format(ati?.avgOvertime ?: 0f)}min,  " +
                        "Padding: ${ati?.predictedPadding ?: 0}min,  " +
                        "Tasks: ${ati?.tasksCompleted ?: 0}"
            } else if (selectedType == "Event" && masterEvents.isNotEmpty()) {
                val evt = masterEvents.getOrNull(selectedEntityIndex) ?: return@launch
                val ati = db.eventATIDao().getById(evt.id)
                val completed = allTasks
                    .filter { it.eventId == evt.id && it.status == 3 && it.allDay == null }
                    .takeLast(10)
                completed.forEach { t ->
                    points.add(Pair(t.predictedDuration.toFloat(), (t.overTime ?: 0).toFloat()))
                }
                atiRecord = "Score: ${"%.3f".format(ati?.score ?: 0f)},  " +
                        "Misses: ${ati?.deadlineMissCount ?: 0},  " +
                        "Avg OT: ${"%.1f".format(ati?.avgOvertime ?: 0f)}min,  " +
                        "Padding: ${ati?.predictedPadding ?: 0}min,  " +
                        "Tasks: ${ati?.tasksCompleted ?: 0}"
            }

            // Calculate regression line for plot
            if (points.size >= 2) {
                val n = points.size.toFloat()
                val sumX = points.sumOf { it.first.toDouble() }.toFloat()
                val sumY = points.sumOf { it.second.toDouble() }.toFloat()
                val sumXY = points.sumOf { (x, y) -> (x * y).toDouble() }.toFloat()
                val sumX2 = points.sumOf { (x, _) -> (x * x).toDouble() }.toFloat()
                val denom = n * sumX2 - sumX * sumX
                slope = if (denom != 0f) (n * sumXY - sumX * sumY) / denom else 0f
                intercept = (sumY - slope * sumX) / n
            } else {
                slope = 0f
                intercept = 0f
            }

            dataPoints = points
        }
    }

    LaunchedEffect(selectedType, selectedEntityIndex, categories, masterEvents) {
        loadData()
    }

    Text(
        "Overtime Predicting Model",
        style = MaterialTheme.typography.titleMedium,
        color = Color.Black
    )
    Spacer(Modifier.height(12.dp))

    // Type toggle + Entity dropdown
    Box(
        modifier = Modifier
            .width(378.dp)
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { type ->
                    WeekButton(
                        label = type,
                        selected = selectedType == type,
                        color = PrimaryColor
                    ) {
                        selectedType = type
                        selectedEntityIndex = 0
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (entityNames.isNotEmpty()) {
                var showDropdown by remember(selectedType) { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDropdown = !showDropdown }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundColor, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(entityNames.getOrNull(selectedEntityIndex) ?: "None", color = Color.Black)
                        }
                        AnimatedVisibility(
                            visible = showDropdown,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .heightIn(max = 180.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                entityNames.forEachIndexed { index, name ->
                                    val isSelected = selectedEntityIndex == index
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(
                                                if (isSelected) PrimaryColor else Color.LightGray,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedEntityIndex = index
                                                showDropdown = false
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            name,
                                            color = if (isSelected) BackgroundColor else Color.Black,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text("No ${selectedType.lowercase()} found", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // ATI stats
    Box(
        modifier = Modifier
            .width(378.dp)
            .background(Color(CardColor), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(if (atiRecord.isNotEmpty()) atiRecord else "—", fontSize = 16.sp, color = Color.Black)
    }

    Spacer(Modifier.height(8.dp))

    // Scatter plot
    val maxX = (dataPoints.maxOfOrNull { it.first } ?: 0f).let { (it * 1.2f).coerceAtLeast(120f) }
    val maxY = (dataPoints.maxOfOrNull { it.second } ?: 0f).let { (it * 1.2f).coerceAtLeast(60f) }
    val dotColor = Color.Gray
    val lineColor = PrimaryColor
    val axisColor = Color.DarkGray
    val gridColor = Color(0xFFE0E0E0)
    val mL = 124f   // margin left
    val mB = 64f   // margin bottom
    val mT = 12f   // margin top
    val mR = 12f   // margin right

    Canvas(
        modifier = Modifier
            .width(370.dp)
            .height(280.dp)
            .background(BackgroundColor)
    ) {
        val W = size.width
        val H = size.height
        val w = W - mL - mR
        val h = H - mB - mT

        fun px(dataVal: Float) = mL + dataVal / maxX * w
        fun py(dataVal: Float) = mT + h - dataVal / maxY * h

        // Grid lines
        for (i in 1..4) {
            val gy = mT + h * (1f - i / 4f)
            drawLine(gridColor, Offset(mL, gy), Offset(mL + w, gy), strokeWidth = 1f)
            val gx = mL + w * i / 4f
            drawLine(gridColor, Offset(gx, mT), Offset(gx, mT + h), strokeWidth = 1f)
        }

        // Axes
        drawLine(axisColor, Offset(mL, mT + h), Offset(mL + w, mT + h), strokeWidth = 2f)
        drawLine(axisColor, Offset(mL, mT), Offset(mL, mT + h), strokeWidth = 2f)

        // Labels
        drawContext.canvas.nativeCanvas.apply {
            val textPx = 14.dp.toPx()
            val tickPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = textPx
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = textPx
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // X tick values
            val xTickY = mT + h + textPx + 6f
            for (i in 0..4) {
                val xVal = (maxX * i / 4f).toInt()
                drawText(xVal.toString(), mL + w * i / 4f, xTickY, tickPaint)
            }

            // X axis label — below ticks
            drawText("Predicted (min)", mL + w / 2f, xTickY + textPx + 8f, labelPaint)

            // Y tick values — right-aligned, flush to axis
            tickPaint.textAlign = android.graphics.Paint.Align.RIGHT
            for (i in 0..4) {
                val yVal = (maxY * i / 4f).toInt()
                drawText(yVal.toString(), mL - 28f, mT + h * (1f - i / 4f) + textPx / 2f, tickPaint)
            }

            // Y axis label — rotated, in far left of margin
            withSave {
                val pivotX = 16f
                rotate(-90f, pivotX, mT + h / 2f)
                labelPaint.textAlign = android.graphics.Paint.Align.CENTER
                drawText("Overtime (min)", pivotX, mT + h / 2f + textPx / 2f, labelPaint)
            }
        }

        // Regression line
        if (dataPoints.size >= 2 && (slope != 0f || intercept != 0f)) {
            drawLine(
                lineColor,
                Offset(px(0f), py((intercept).coerceIn(0f, maxY))),
                Offset(px(maxX), py((slope * maxX + intercept).coerceIn(0f, maxY))),
                strokeWidth = 2f
            )
        }

        // Data points
        dataPoints.forEach { (x, y) ->
            drawCircle(dotColor, radius = 8f, center = Offset(px(x), py(y)))
        }
    }

    Spacer(Modifier.height(32.dp))
}