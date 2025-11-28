package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp

/* CONSTANTS */
private val COLUMN_WIDTH = 110.dp
private val HEADER_BG = Color.LightGray
private val GRID_COLOR = Color.LightGray
private val buttonShape = RoundedCornerShape(10.dp)

/* DATABASE PREVIEW */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Developer() {
    val scrollStateVertical = rememberScrollState()
    val scrollStateHorizontal = rememberScrollState()

    // DB
    val context = LocalContext.current
    val db = AppDatabaseProvider.getDatabase(context)

    // State holders
    var categories by remember { mutableStateOf(listOf<Category>()) }
    var events by remember { mutableStateOf(listOf<MasterEvent>()) }
    var deadlines by remember { mutableStateOf(listOf<Deadline>()) }
    var buckets by remember { mutableStateOf(listOf<MasterTaskBucket>()) }
    var tasks by remember { mutableStateOf(listOf<MasterTask>()) }
    var reminders by remember { mutableStateOf(listOf<Reminder>()) }

    // Load DB
    fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            categories = db.categoryDao().getAll()
            events = db.eventDao().getAllMasterEvents()
            deadlines = db.deadlineDao().getAll()
            buckets = db.taskBucketDao().getAllMasterBuckets()
            tasks = db.taskDao().getAllMasterTasks()
            reminders = db.reminderDao().getAll()
        }
    }
    LaunchedEffect(Unit) { refreshData() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollStateVertical)
            .horizontalScroll(scrollStateHorizontal)
            .padding(8.dp)
    ) {
        // Buttons
        Row(modifier = Modifier.padding(bottom = 16.dp)) {

            // Clear button
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        db.reminderDao().getAll().forEach { db.reminderDao().deleteById(it.id) }
                        db.taskDao().getAllMasterTasks().forEach { db.taskDao().deleteMasterTask(it.id) }
                        db.taskBucketDao().getAllMasterBuckets().forEach { db.taskBucketDao().deleteMasterBucket(it.id) }
                        db.deadlineDao().getAll().forEach { db.deadlineDao().deleteById(it.id) }
                        db.eventDao().getAllMasterEvents().forEach { db.eventDao().deleteMasterEvent(it.id) }
                        db.categoryDao().getAll().forEach { db.categoryDao().deleteById(it.id) }
                        refreshData()
                    }
                },
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = BackgroundColor
                )
            ) {
                Text("Clear", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Dummy button
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        runDummy(db)
                        refreshData()
                    }
                },
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = BackgroundColor
                )
            ) {
                Text("Dummy", fontSize = 16.sp)
            }
        }

        // Database view table
        @Composable
        fun <T> Table(
            title: String,
            data: List<T>,
            headers: List<String>,
            rowContent: (T) -> List<String>
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.Black)
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .border(1.dp, GRID_COLOR)
            ) {
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
                                    if (index < headers.lastIndex) {
                                        drawLine(
                                            color = GRID_COLOR,
                                            start = Offset(size.width, 0f),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Text(header, color = Color.Black)
                        }
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
                                        if (index < values.lastIndex) {
                                            drawLine(
                                                color = GRID_COLOR,
                                                start = Offset(size.width, 0f),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(value, color = Color.Black)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Tables
        Table(
            title = "Categories",
            data = categories,
            headers = listOf("ID", "Title", "Notes", "Color")
        ) { c -> listOf(c.id.toString(), c.title, c.notes ?: "", c.color) }

        Table(
            title = "Events",
            data = events,
            headers = listOf("ID", "Title", "Notes", "Color", "StartDate", "EndDate", "StartTime", "EndTime", "Freq", "CategoryId")
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
                e.categoryId?.toString() ?: ""
            )
        }

        Table(
            title = "Deadlines",
            data = deadlines,
            headers = listOf("ID", "Title", "Notes", "Date", "Time", "CategoryId", "EventId")
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
            title = "Task Buckets",
            data = buckets,
            headers = listOf("ID", "StartDate", "EndDate", "StartTime", "EndTime", "Freq")
        ) { b ->
            listOf(
                b.id.toString(),
                b.startDate.toString(),
                b.endDate?.toString() ?: "",
                b.startTime.toString(),
                b.endTime.toString(),
                b.recurFreq.name
            )
        }

        Table(
            title = "Tasks",
            data = tasks,
            headers = listOf("ID", "Title", "Notes", "Priority", "Breakable", "NoIntervals")
        ) { t ->
            listOf(
                t.id.toString(),
                t.title,
                t.notes ?: "",
                t.priority.toString(),
                t.breakable?.toString() ?: "",
                t.noIntervals.toString()
            )
        }

        Table(
            title = "Reminders",
            data = reminders,
            headers = listOf("ID", "Title", "Notes", "Color", "Date", "Time", "AllDay")
        ) { r ->
            listOf(
                r.id.toString(),
                r.title,
                r.notes ?: "",
                r.color,
                r.date.toString(),
                r.time.toString(),
                r.allDay?.toString() ?: ""
            )
        }
    }
}