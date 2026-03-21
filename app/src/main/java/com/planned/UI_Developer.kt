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
import androidx.compose.ui.draw.clip
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

data class ScheduleOrderRow(
    val masterTaskId: Int,
    val title: String,
    val dependencyTaskId: Int?,
    val urgency: Int?,
    val categoryScore: Float,
    val eventScore: Float
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Developer(db: AppDatabase) {
    var currentView by remember { mutableStateOf("main") }

    // Shared DB state — loaded once here and passed down so sub-pages
    // don't each trigger their own independent loads.
    val context = LocalContext.current
    val database = AppDatabaseProvider.getDatabase(context)

    var categories        by remember { mutableStateOf(listOf<Category>()) }
    var masterEvents      by remember { mutableStateOf(listOf<MasterEvent>()) }
    var eventOccurrences  by remember { mutableStateOf(listOf<EventOccurrence>()) }
    var deadlines         by remember { mutableStateOf(listOf<Deadline>()) }
    var masterBuckets     by remember { mutableStateOf(listOf<MasterTaskBucket>()) }
    var bucketOccurrences by remember { mutableStateOf(listOf<TaskBucketOccurrence>()) }
    var masterTasks       by remember { mutableStateOf(listOf<MasterTask>()) }
    var taskIntervals     by remember { mutableStateOf(listOf<TaskInterval>()) }
    var masterReminders   by remember { mutableStateOf(listOf<MasterReminder>()) }
    var reminderOccurrences by remember { mutableStateOf(listOf<ReminderOccurrence>()) }
    var settings          by remember { mutableStateOf(listOf<AppSetting>()) }
    var categoryATIList   by remember { mutableStateOf(listOf<CategoryATI>()) }
    var eventATIList      by remember { mutableStateOf(listOf<EventATI>()) }
    var scheduleOrderRows by remember { mutableStateOf(listOf<ScheduleOrderRow>()) }

    fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            categories        = database.categoryDao().getAll()
            masterEvents      = database.eventDao().getAllMasterEvents()
            eventOccurrences  = database.eventDao().getAllOccurrences()
            deadlines         = database.deadlineDao().getAll()
            masterBuckets     = database.taskBucketDao().getAllMasterBuckets()
            bucketOccurrences = database.taskBucketDao().getAllBucketOccurrences()
            masterTasks       = database.taskDao().getAllMasterTasks()
            taskIntervals     = database.taskDao().getAllIntervals()
            masterReminders   = database.reminderDao().getAllMasterReminders()
            reminderOccurrences = database.reminderDao().getAllOccurrences()
            database.settingsDao().getAll()?.let { settings = listOf(it) } ?: run { settings = emptyList() }
            categoryATIList   = database.categoryATIDao().getAll()
            eventATIList      = database.eventATIDao().getAll()

            // Build schedule order rows
            val today = java.time.LocalDate.now()
            val allDeadlines = database.deadlineDao().getAll()
            val autoTasks = database.taskDao().getAllMasterTasks().filter {
                it.status != 3 && it.allDay == null && it.startDate == null && it.startTime == null
            }
            val rows = mutableListOf<ScheduleOrderRow>()
            for (task in autoTasks) {
                val deadline = task.deadlineId?.let { id -> allDeadlines.find { it.id == id } }
                val urgency  = deadline?.let {
                    java.time.temporal.ChronoUnit.DAYS.between(today, it.date).toInt()
                }
                val categoryScore = task.categoryId?.let { id -> categoryATIList.find { it.categoryId == id }?.score } ?: 0f
                val eventScore    = task.eventId?.let { id -> eventATIList.find { it.eventId == id }?.score } ?: 0f
                rows.add(ScheduleOrderRow(
                    masterTaskId     = task.id,
                    title            = task.title,
                    dependencyTaskId = task.dependencyTaskId,
                    urgency          = urgency,
                    categoryScore    = categoryScore,
                    eventScore       = eventScore
                ))
            }
            scheduleOrderRows = rows.sortedWith(compareBy(
                { it.urgency ?: Int.MAX_VALUE },
                { -(it.categoryScore) },
                { -(it.eventScore) },
                { it.masterTaskId }
            ))
        }
    }
    LaunchedEffect(Unit) { refreshData() }

    when (currentView) {
        "main" -> DeveloperMainView(
            db            = database,
            onATIClick      = { currentView = "ati" },
            onDatabaseClick = { currentView = "database" },
            onScheduleClick = { currentView = "schedule" },
            refreshData   = { refreshData() }
        )
        "ati" -> DeveloperATIPage(
            db           = database,
            categories   = categories,
            masterEvents = masterEvents,
            onBack       = { currentView = "main" }
        )
        "database" -> DeveloperDatabasePage(
            categories        = categories,
            masterEvents      = masterEvents,
            eventOccurrences  = eventOccurrences,
            deadlines         = deadlines,
            masterBuckets     = masterBuckets,
            bucketOccurrences = bucketOccurrences,
            masterTasks       = masterTasks,
            taskIntervals     = taskIntervals,
            masterReminders   = masterReminders,
            reminderOccurrences = reminderOccurrences,
            categoryATIList   = categoryATIList,
            eventATIList      = eventATIList,
            settings          = settings,
            onBack            = { currentView = "main" }
        )
        "schedule" -> DeveloperSchedulePage(
            scheduleOrderRows = scheduleOrderRows,
            onBack            = { currentView = "main" }
        )
    }
}

/* Main View */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeveloperMainView(
    db: AppDatabase,
    onATIClick: () -> Unit,
    onDatabaseClick: () -> Unit,
    onScheduleClick: () -> Unit,
    refreshData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(12.dp)
    ) {
        // ── Combined card ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "Occurrence Window (Months)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { if (generationMonths > 1) generationMonths-- },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor,
                            contentColor   = BackgroundColor
                        ),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("-", fontSize = 20.sp) }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .background(BackgroundColor, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(generationMonths.toString(), fontSize = 18.sp, color = Color.Black)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { if (generationMonths < 24) generationMonths++ },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor,
                            contentColor   = BackgroundColor
                        ),
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("+", fontSize = 20.sp) }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.LightGray)

                Text(
                    "Database State",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                    ) { Text("Clear", fontSize = 16.sp) }

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
                    ) { Text("Sample", fontSize = 16.sp) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 3 nav items ───────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DeveloperNavItem(label = "ATI Model",  onClick = onATIClick)
            DeveloperNavItem(label = "Database",   onClick = onDatabaseClick)
            DeveloperNavItem(label = "Schedule",   onClick = onScheduleClick)
        }
    }
}

@Composable
fun DeveloperNavItem(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

/* ATI Model */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeveloperATIPage(
    db: AppDatabase,
    categories: List<Category>,
    masterEvents: List<MasterEvent>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Back button — always visible, outside any scroll
        Box(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        // ATI content — no scroll states, fillMaxWidth
        ATIScatterPlot(
            db           = db,
            categories   = categories,
            masterEvents = masterEvents
        )
    }
}

/* Database */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeveloperDatabasePage(
    categories: List<Category>,
    masterEvents: List<MasterEvent>,
    eventOccurrences: List<EventOccurrence>,
    deadlines: List<Deadline>,
    masterBuckets: List<MasterTaskBucket>,
    bucketOccurrences: List<TaskBucketOccurrence>,
    masterTasks: List<MasterTask>,
    taskIntervals: List<TaskInterval>,
    masterReminders: List<MasterReminder>,
    reminderOccurrences: List<ReminderOccurrence>,
    categoryATIList: List<CategoryATI>,
    eventATIList: List<EventATI>,
    settings: List<AppSetting>,
    onBack: () -> Unit
) {
    val scrollV = rememberScrollState()
    val scrollH = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Back button — fixed, outside scroll
        Box(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        // Scrollable table content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollV)
                .horizontalScroll(scrollH)
                .padding(8.dp)
        ) {
            fun formatRecurrenceRule(rule: RecurrenceRule, freq: RecurrenceFrequency): String {
                return when (freq) {
                    RecurrenceFrequency.NONE, RecurrenceFrequency.DAILY -> ""
                    RecurrenceFrequency.WEEKLY  -> rule.daysOfWeek?.joinToString(", ") ?: ""
                    RecurrenceFrequency.MONTHLY -> rule.daysOfMonth?.joinToString(", ") ?: ""
                    RecurrenceFrequency.YEARLY  -> rule.monthAndDay?.let { "${it.first}/${it.second}" } ?: ""
                }
            }

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
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(HEADER_BG)
                            .fillMaxWidth()
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
                                            color       = GRID_COLOR,
                                            start       = Offset(size.width, 0f),
                                            end         = Offset(size.width, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                    .padding(8.dp)
                            ) { Text(header, color = Color.Black) }
                        }
                    }
                    // Data rows
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
                                        color       = GRID_COLOR,
                                        start       = Offset(0f, size.height),
                                        end         = Offset(size.width, size.height),
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
                                                color       = GRID_COLOR,
                                                start       = Offset(size.width, 0f),
                                                end         = Offset(size.width, size.height),
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

            Table(
                title   = "Categories",
                data    = categories,
                headers = listOf("ID", "Title", "Notes", "Color")
            ) { c -> listOf(c.id.toString(), c.title, c.notes ?: "", c.color) }

            Table(
                title   = "Master Events",
                data    = masterEvents,
                headers = listOf(
                    "ID", "Title", "Notes", "Color",
                    "Start Date", "End Date", "Start Time", "End Time",
                    "Frequency", "Rule", "Category ID"
                )
            ) { e ->
                listOf(
                    e.id.toString(), e.title, e.notes ?: "", e.color ?: "",
                    e.startDate.toString(), e.endDate?.toString() ?: "",
                    e.startTime.toString(), e.endTime.toString(),
                    e.recurFreq.name,
                    formatRecurrenceRule(e.recurRule, e.recurFreq),
                    e.categoryId?.toString() ?: ""
                )
            }

            Table(
                title   = "Event Occurrences",
                data    = eventOccurrences,
                headers = listOf("ID", "Master Event ID", "Notes", "Occur Date", "Start Time", "End Time")
            ) { o ->
                listOf(
                    o.id.toString(), o.masterEventId.toString(),
                    o.notes ?: "", o.occurDate.toString(),
                    o.startTime.toString(), o.endTime.toString()
                )
            }

            Table(
                title   = "Deadlines",
                data    = deadlines,
                headers = listOf("ID", "Title", "Notes", "Date", "Time", "Category ID", "Event ID")
            ) { d ->
                listOf(
                    d.id.toString(), d.title, d.notes ?: "",
                    d.date.toString(), d.time.toString(),
                    d.categoryId?.toString() ?: "", d.eventId?.toString() ?: ""
                )
            }

            Table(
                title   = "Master Task Buckets",
                data    = masterBuckets,
                headers = listOf("ID", "Start Date", "End Date", "Start Time", "End Time", "Frequency", "Rule")
            ) { b ->
                listOf(
                    b.id.toString(),
                    b.startDate.toString(), b.endDate?.toString() ?: "",
                    b.startTime.toString(), b.endTime.toString(),
                    b.recurFreq.name,
                    formatRecurrenceRule(b.recurRule, b.recurFreq)
                )
            }

            Table(
                title   = "Task Bucket Occurrences",
                data    = bucketOccurrences,
                headers = listOf("ID", "Master Bucket ID", "Occur Date", "Start Time", "End Time", "Is Exception")
            ) { o ->
                listOf(
                    o.id.toString(), o.masterBucketId.toString(),
                    o.occurDate.toString(), o.startTime.toString(),
                    o.endTime.toString(), o.isException.toString()
                )
            }

            Table(
                title   = "Master Tasks",
                data    = masterTasks,
                headers = listOf(
                    "ID", "Title", "Notes", "All Day", "Breakable", "No. Intervals",
                    "Start Date", "Start Time", "Duration", "Actual Duration",
                    "Status", "Time Left", "Overtime",
                    "Category ID", "Event ID", "Deadline ID", "Dependency Task ID"
                )
            ) { t ->
                listOf(
                    t.id.toString(), t.title, t.notes ?: "",
                    t.allDay?.toString() ?: "", t.breakable?.toString() ?: "",
                    t.noIntervals.toString(),
                    t.startDate?.toString() ?: "", t.startTime?.toString() ?: "",
                    t.predictedDuration.toString(), t.actualDuration?.toString() ?: "",
                    t.status?.toString() ?: "", t.timeLeft?.toString() ?: "",
                    t.overTime?.toString() ?: "",
                    t.categoryId?.toString() ?: "", t.eventId?.toString() ?: "",
                    t.deadlineId?.toString() ?: "", t.dependencyTaskId?.toString() ?: ""
                )
            }

            Table(
                title   = "Task Intervals",
                data    = taskIntervals,
                headers = listOf(
                    "ID", "Master Task ID", "Interval No.", "Notes",
                    "Occur Date", "Start Time", "End Time",
                    "Status", "Time Left", "Overtime", "ATI Padding"
                )
            ) { i ->
                listOf(
                    i.id.toString(), i.masterTaskId.toString(),
                    i.intervalNo.toString(), i.notes ?: "",
                    i.occurDate.toString(), i.startTime.toString(), i.endTime.toString(),
                    i.status?.toString() ?: "", i.timeLeft?.toString() ?: "",
                    i.overTime?.toString() ?: "", i.atiPadding.toString()
                )
            }

            Table(
                title   = "Master Reminders",
                data    = masterReminders,
                headers = listOf(
                    "ID", "Title", "Notes", "Start Date", "End Date",
                    "Time", "All Day", "Frequency", "Rule", "Category ID"
                )
            ) { r ->
                listOf(
                    r.id.toString(), r.title, r.notes ?: "",
                    r.startDate.toString(), r.endDate?.toString() ?: "",
                    r.time?.toString() ?: "", r.allDay.toString(),
                    r.recurFreq.name,
                    formatRecurrenceRule(r.recurRule, r.recurFreq),
                    r.categoryId?.toString() ?: ""
                )
            }

            Table(
                title   = "Reminder Occurrences",
                data    = reminderOccurrences,
                headers = listOf("ID", "Master Reminder ID", "Notes", "Occur Date", "Time", "All Day")
            ) { o ->
                listOf(
                    o.id.toString(), o.masterReminderId.toString(),
                    o.notes ?: "", o.occurDate.toString(),
                    o.time?.toString() ?: "", o.allDay.toString()
                )
            }

            Table(
                title   = "Category ATI",
                data    = categoryATIList,
                headers = listOf(
                    "Category ID", "Score", "Deadline Misses",
                    "Avg Overtime", "Tasks Completed", "Predicted Padding", "Slope", "Intercept"
                )
            ) { a ->
                listOf(
                    a.categoryId.toString(), "%.3f".format(a.score),
                    a.deadlineMissCount.toString(), "%.1f".format(a.avgOvertime),
                    a.tasksCompleted.toString(), a.predictedPadding.toString(),
                    "%.3f".format(a.paddingSlope), "%.3f".format(a.paddingIntercept)
                )
            }

            Table(
                title   = "Event ATI",
                data    = eventATIList,
                headers = listOf(
                    "Event ID", "Score", "Deadline Misses",
                    "Avg Overtime", "Tasks Completed", "Predicted Padding", "Slope", "Intercept"
                )
            ) { a ->
                listOf(
                    a.eventId.toString(), "%.3f".format(a.score),
                    a.deadlineMissCount.toString(), "%.1f".format(a.avgOvertime),
                    a.tasksCompleted.toString(), a.predictedPadding.toString(),
                    "%.3f".format(a.paddingSlope), "%.3f".format(a.paddingIntercept)
                )
            }

            Table(
                title   = "Settings",
                data    = settings,
                headers = listOf(
                    "ID", "Start Week On Monday", "Primary Color",
                    "Show Developer", "Break Duration", "Break Every"
                )
            ) { s ->
                listOf(
                    s.id.toString(), s.startWeekOnMonday.toString(),
                    s.primaryColor, s.showDeveloper.toString(),
                    s.breakDuration.toString(), s.breakEvery.toString()
                )
            }
        }
    }
}

/* Schedule */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeveloperSchedulePage(
    scheduleOrderRows: List<ScheduleOrderRow>,
    onBack: () -> Unit
) {
    val scrollV = rememberScrollState()
    val scrollH = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Back button — fixed, outside scroll
        Box(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        // Scrollable schedule table
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollV)
                .horizontalScroll(scrollH)
                .padding(8.dp)
        ) {
            Text(
                "Task Schedule Order",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.border(1.dp, GRID_COLOR)) {
                // Header
                Row(
                    modifier = Modifier
                        .background(HEADER_BG)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    listOf("Task ID", "Title", "Dependency", "Urgency", "Cat. Score", "Event Score")
                        .forEachIndexed { index, header ->
                            Box(
                                modifier = Modifier
                                    .width(COLUMN_WIDTH)
                                    .fillMaxHeight()
                                    .drawWithContent {
                                        drawContent()
                                        if (index < 5) drawLine(
                                            color       = GRID_COLOR,
                                            start       = Offset(size.width, 0f),
                                            end         = Offset(size.width, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                    .padding(8.dp)
                            ) { Text(header, color = Color.Black) }
                        }
                }
                // Data rows
                val displayRows = scheduleOrderRows.ifEmpty { listOf(null) }
                displayRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .drawWithContent {
                                drawContent()
                                drawLine(
                                    color       = GRID_COLOR,
                                    start       = Offset(0f, size.height),
                                    end         = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                    ) {
                        val values = if (row != null) listOf(
                            row.masterTaskId.toString(),
                            row.title,
                            row.dependencyTaskId?.toString() ?: "",
                            row.urgency?.toString() ?: "",
                            "%.3f".format(row.categoryScore),
                            "%.3f".format(row.eventScore)
                        ) else List(6) { "" }

                        values.forEachIndexed { index, value ->
                            Box(
                                modifier = Modifier
                                    .width(COLUMN_WIDTH)
                                    .fillMaxHeight()
                                    .drawWithContent {
                                        drawContent()
                                        if (index < 5) drawLine(
                                            color       = GRID_COLOR,
                                            start       = Offset(size.width, 0f),
                                            end         = Offset(size.width, size.height),
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
    }
}

/* ATI Scatter Plot */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ATIScatterPlot(
    db: AppDatabase,
    categories: List<Category>,
    masterEvents: List<MasterEvent>
) {
    val scope = rememberCoroutineScope()

    var selectedType        by remember { mutableStateOf("Category") }
    val types               = listOf("Category", "Event")
    var selectedEntityIndex by remember { mutableIntStateOf(0) }
    var dataPoints          by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var atiRecord           by remember { mutableStateOf("") }
    var slope               by remember { mutableFloatStateOf(0f) }
    var intercept           by remember { mutableFloatStateOf(0f) }

    val entityNames = if (selectedType == "Category")
        categories.map { it.title }
    else
        masterEvents.map { it.title }

    fun loadData() {
        scope.launch(Dispatchers.IO) {
            val allTasks = db.taskDao().getAllMasterTasks()
            val points   = mutableListOf<Pair<Float, Float>>()

            if (selectedType == "Category" && categories.isNotEmpty()) {
                val cat = categories.getOrNull(selectedEntityIndex) ?: return@launch
                val ati = db.categoryATIDao().getById(cat.id)
                allTasks
                    .filter { it.categoryId == cat.id && it.status == 3 && it.allDay == null }
                    .takeLast(10)
                    .forEach { t -> points.add(Pair(t.predictedDuration.toFloat(), (t.overTime ?: 0).toFloat())) }
                atiRecord = "Score: ${"%.3f".format(ati?.score ?: 0f)},  " +
                        "Misses: ${ati?.deadlineMissCount ?: 0},  " +
                        "Avg OT: ${"%.1f".format(ati?.avgOvertime ?: 0f)}min,  " +
                        "Padding: ${ati?.predictedPadding ?: 0}min,  " +
                        "Slope: ${"%.3f".format(ati?.paddingSlope ?: 0f)},  " +
                        "Intercept: ${"%.3f".format(ati?.paddingIntercept ?: 0f)},  " +
                        "Tasks: ${ati?.tasksCompleted ?: 0}"
            } else if (selectedType == "Event" && masterEvents.isNotEmpty()) {
                val evt = masterEvents.getOrNull(selectedEntityIndex) ?: return@launch
                val ati = db.eventATIDao().getById(evt.id)
                allTasks
                    .filter { it.eventId == evt.id && it.status == 3 && it.allDay == null }
                    .takeLast(10)
                    .forEach { t -> points.add(Pair(t.predictedDuration.toFloat(), (t.overTime ?: 0).toFloat())) }
                atiRecord = "Score: ${"%.3f".format(ati?.score ?: 0f)},  " +
                        "Misses: ${ati?.deadlineMissCount ?: 0},  " +
                        "Avg OT: ${"%.1f".format(ati?.avgOvertime ?: 0f)}min,  " +
                        "Padding: ${ati?.predictedPadding ?: 0}min,  " +
                        "Slope: ${"%.3f".format(ati?.paddingSlope ?: 0f)},  " +
                        "Intercept: ${"%.3f".format(ati?.paddingIntercept ?: 0f)},  " +
                        "Tasks: ${ati?.tasksCompleted ?: 0}"
            }

            if (points.size >= 2) {
                val n     = points.size.toFloat()
                val sumX  = points.sumOf { it.first.toDouble() }.toFloat()
                val sumY  = points.sumOf { it.second.toDouble() }.toFloat()
                val sumXY = points.sumOf { (x, y) -> (x * y).toDouble() }.toFloat()
                val sumX2 = points.sumOf { (x, _) -> (x * x).toDouble() }.toFloat()
                val denom = n * sumX2 - sumX * sumX
                slope     = if (denom != 0f) (n * sumXY - sumX * sumY) / denom else 0f
                intercept = (sumY - slope * sumX) / n
            } else {
                slope     = 0f
                intercept = 0f
            }
            dataPoints = points
        }
    }

    LaunchedEffect(selectedType, selectedEntityIndex, categories, masterEvents) { loadData() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            "Overtime Regression Model",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black
        )
        Spacer(Modifier.height(12.dp))

        // Type toggle + entity dropdown card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { type ->
                        WeekButton(
                            label    = type,
                            selected = selectedType == type,
                            color    = PrimaryColor
                        ) {
                            selectedType        = type
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
                                indication        = null
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
                                enter   = fadeIn() + expandVertically(),
                                exit    = fadeOut() + shrinkVertically()
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
                                                    showDropdown        = false
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                name,
                                                color      = if (isSelected) BackgroundColor else Color.Black,
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

        // ATI stats card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(
                if (atiRecord.isNotEmpty()) atiRecord else "Stats Unavailable",
                fontSize = 16.sp,
                color    = Color.Black
            )
        }

        Spacer(Modifier.height(8.dp))

        // Scatter plot — fillMaxWidth, fixed height
        val maxX      = (dataPoints.maxOfOrNull { it.first } ?: 0f).let { (it * 1.2f).coerceAtLeast(120f) }
        val maxY      = (dataPoints.maxOfOrNull { it.second } ?: 0f).let { (it * 1.2f).coerceAtLeast(60f) }
        val dotColor  = Color.Gray
        val lineColor = PrimaryColor
        val axisColor = Color.DarkGray
        val gridColor = Color(0xFFE0E0E0)
        val mL = 124f
        val mB = 64f
        val mT = 12f
        val mR = 12f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
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
                val textPx     = 14.dp.toPx()
                val tickPaint  = android.graphics.Paint().apply {
                    color     = android.graphics.Color.DKGRAY
                    textSize  = textPx
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val labelPaint = android.graphics.Paint().apply {
                    color     = android.graphics.Color.DKGRAY
                    textSize  = textPx
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val xTickY = mT + h + textPx + 6f
                for (i in 0..4) {
                    val xVal = (maxX * i / 4f).toInt()
                    drawText(xVal.toString(), mL + w * i / 4f, xTickY, tickPaint)
                }
                drawText("Predicted (min)", mL + w / 2f, xTickY + textPx + 8f, labelPaint)

                tickPaint.textAlign = android.graphics.Paint.Align.RIGHT
                for (i in 0..4) {
                    val yVal = (maxY * i / 4f).toInt()
                    drawText(yVal.toString(), mL - 28f, mT + h * (1f - i / 4f) + textPx / 2f, tickPaint)
                }

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
                    Offset(px(0f), py(intercept.coerceIn(0f, maxY))),
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
}