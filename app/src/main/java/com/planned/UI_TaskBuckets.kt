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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TaskBuckets(db: AppDatabase) {
    var currentView by remember { mutableStateOf("list") }
    var selectedBucket by remember { mutableStateOf<MasterTaskBucket?>(null) }

    when (currentView) {
        "list" -> TaskBucketsList(
            db = db,
            onBucketClick = { bucket ->
                selectedBucket = bucket
                currentView = "info"
            }
        )
        "info" -> selectedBucket?.let { bucket ->
            TaskBucketInfoPage(
                db = db,
                bucket = bucket,
                onBack = {
                    currentView = "list"
                    selectedBucket = null
                },
                onUpdate = { currentView = "update" }
            )
        }
        "update" -> selectedBucket?.let { bucket ->
            TaskBucketUpdateForm(
                db = db,
                bucket = bucket,
                onBack = { currentView = "info" },
                onSaveSuccess = { updatedBucket ->
                    selectedBucket = updatedBucket
                    currentView = "info"
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskBucketsList(
    db: AppDatabase,
    onBucketClick: (MasterTaskBucket) -> Unit
) {
    var buckets by remember { mutableStateOf<List<MasterTaskBucket>>(emptyList()) }

    LaunchedEffect(Unit) {
        buckets = TaskBucketManager.getAll(db)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Text(
            "Task Buckets",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (buckets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No task buckets",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(buckets) { bucket ->
                    TaskBucketListItem(
                        bucket = bucket,
                        onClick = { onBucketClick(bucket) }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskBucketListItem(
    bucket: MasterTaskBucket,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${bucket.startTime} - ${bucket.endTime}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = bucket.recurFreq.name,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskBucketInfoPage(
    db: AppDatabase,
    bucket: MasterTaskBucket,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var currentBucket by remember { mutableStateOf(bucket) }

    LaunchedEffect(bucket.id) {
        currentBucket = db.taskBucketDao().getMasterBucketById(bucket.id) ?: bucket
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(text = "Task Bucket Details", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoField("Start Date", currentBucket.startDate.toString())
                InfoField("End Date", currentBucket.endDate?.toString() ?: "N/A")
                InfoField("Start Time", currentBucket.startTime.toString())
                InfoField("End Time", currentBucket.endTime.toString())
                InfoField("Recurrence", currentBucket.recurFreq.name)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        TaskBucketManager.delete(db, currentBucket.id)
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskBucketUpdateForm(
    db: AppDatabase,
    bucket: MasterTaskBucket,
    onBack: () -> Unit,
    onSaveSuccess: (MasterTaskBucket) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var startDate by remember { mutableStateOf(bucket.startDate) }
    var endDate by remember { mutableStateOf(bucket.endDate) }
    var startTime by remember { mutableStateOf(bucket.startTime) }
    var endTime by remember { mutableStateOf(bucket.endTime) }
    var recurrenceFreq by remember { mutableStateOf(bucket.recurFreq) }
    var selectedDaysOfWeek by remember { mutableStateOf(bucket.recurRule.daysOfWeek?.toSet() ?: setOf(7)) }
    var selectedDaysOfMonth by remember { mutableStateOf(bucket.recurRule.daysOfMonth?.toSet() ?: setOf(1)) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            TaskBucketForm(
                db = db,
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                startTime = startTime,
                onStartTimeChange = { startTime = it },
                endTime = endTime,
                onEndTimeChange = { endTime = it },
                recurrenceFreq = recurrenceFreq,
                selectedDaysOfWeek = selectedDaysOfWeek,
                selectedDaysOfMonth = selectedDaysOfMonth,
                onRecurrenceChange = { freq, daysWeek, daysMonth, endDateVal ->
                    recurrenceFreq = freq
                    selectedDaysOfWeek = daysWeek
                    selectedDaysOfMonth = daysMonth
                    endDate = endDateVal
                },
                resetTrigger = resetTrigger
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        startDate = bucket.startDate
                        endDate = bucket.endDate
                        startTime = bucket.startTime
                        endTime = bucket.endTime
                        resetTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Reset", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val recurRule = when (recurrenceFreq) {
                                RecurrenceFrequency.NONE -> RecurrenceRule()
                                RecurrenceFrequency.DAILY -> RecurrenceRule()
                                RecurrenceFrequency.WEEKLY -> RecurrenceRule(daysOfWeek = selectedDaysOfWeek.toList())
                                RecurrenceFrequency.MONTHLY -> RecurrenceRule(daysOfMonth = selectedDaysOfMonth.toList())
                                RecurrenceFrequency.YEARLY -> RecurrenceRule(monthAndDay = Pair(startDate.dayOfMonth, startDate.monthValue))
                            }

                            val updatedBucket = bucket.copy(
                                startDate = startDate,
                                endDate = endDate,
                                startTime = startTime,
                                endTime = endTime,
                                recurFreq = recurrenceFreq,
                                recurRule = recurRule
                            )
                            TaskBucketManager.update(db, updatedBucket)
                            val refreshedBucket = db.taskBucketDao().getMasterBucketById(bucket.id) ?: updatedBucket
                            onSaveSuccess(refreshedBucket)
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
}