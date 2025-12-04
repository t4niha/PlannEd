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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* CONSTANTS */
private val COLUMN_WIDTH = 120.dp
private val HEADER_BG = Color.LightGray
private val GRID_COLOR = Color.LightGray
private val buttonShape = RoundedCornerShape(10.dp)

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
                // Generation Months Adjuster
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

            // Clear and Sample Buttons
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
                    "End Time",
                    "Is Exception"
                )
            ) { o ->
                listOf(
                    o.id.toString(),
                    o.masterEventId.toString(),
                    o.notes ?: "",
                    o.occurDate.toString(),
                    o.startTime.toString(),
                    o.endTime.toString(),
                    o.isException.toString()
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
                    "Priority",
                    "Breakable",
                    "No. Intervals",
                    "Start Date",
                    "Start Time",
                    "Duration",
                    "Category ID",
                    "Event ID",
                    "Deadline ID",
                    "Bucket ID"
                )
            ) { t ->
                listOf(
                    t.id.toString(),
                    t.title,
                    t.notes ?: "",
                    t.priority.toString(),
                    t.breakable?.toString() ?: "",
                    t.noIntervals.toString(),
                    t.startDate?.toString() ?: "",
                    t.startTime?.toString() ?: "",
                    t.predictedDuration.toString(),
                    t.categoryId?.toString() ?: "",
                    t.eventId?.toString() ?: "",
                    t.deadlineId?.toString() ?: "",
                    t.bucketId?.toString() ?: ""
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
                    "Status"
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
                    i.status?.toString() ?: ""
                )
            }

            Table(
                title = "Master Reminders",
                data = masterReminders,
                headers = listOf(
                    "ID",
                    "Title",
                    "Notes",
                    "Color",
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
                    r.color,
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
                    "All Day",
                    "Is Exception"
                )
            ) { o ->
                listOf(
                    o.id.toString(),
                    o.masterReminderId.toString(),
                    o.notes ?: "",
                    o.occurDate.toString(),
                    o.time?.toString() ?: "",
                    o.allDay.toString(),
                    o.isException.toString()
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