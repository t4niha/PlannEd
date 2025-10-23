package com.planned

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalDensity
import com.planned.ui.theme.PlanEdTheme
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// UI Constants
private val PrimaryColor = Color(0xFF1976D2)
private val ButtonShape = RoundedCornerShape(20.dp)
private val CircleShapePrimary = CircleShape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanEdTheme {
                HomeScreen()
            }
        }
    }
}

// Homescreen Layout
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var currentView by remember { mutableStateOf("Day") }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var displayedDate by remember { mutableStateOf(today) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PlannEd", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create", modifier = Modifier.size(32.dp))
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // View Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val buttonWidth = 110.dp
                    listOf("Day", "Week", "Month").forEach { view ->
                        val selected = view == currentView
                        Button(
                            onClick = {
                                currentView = view
                                displayedDate = today
                                selectedDate = today
                            },
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) PrimaryColor else Color.LightGray,
                                contentColor = if (selected) Color.White else Color.Black
                            ),
                            modifier = Modifier
                                .width(buttonWidth)
                                .padding(horizontal = 5.dp)
                        ) {
                            Text(view, fontSize = 16.sp)
                        }
                    }
                }

                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (currentView) {
                            "Day" -> displayedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                            "Week" -> {
                                val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                                val endOfWeek = displayedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                                val startMonth = startOfWeek.format(DateTimeFormatter.ofPattern("MMM"))
                                val endMonth = endOfWeek.format(DateTimeFormatter.ofPattern("MMM"))
                                if (startOfWeek.month == endOfWeek.month)
                                    "$startMonth ${startOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth}, ${endOfWeek.year}"
                                else
                                    "$startMonth ${startOfWeek.dayOfMonth} - $endMonth ${endOfWeek.dayOfMonth}, ${endOfWeek.year}"
                            }
                            "Month" -> displayedDate.format(DateTimeFormatter.ofPattern("MMMM, yyyy"))
                            else -> ""
                        },
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                // Calendar Views
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentView) {
                        "Day" -> DayView(displayedDate)
                        "Week" -> WeekView(displayedDate)
                        "Month" -> MonthView(displayedDate, selectedDate) { selectedDate = it }
                    }
                }

                // Navigation Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            displayedDate = when (currentView) {
                                "Day" -> displayedDate.minusDays(1)
                                "Week" -> displayedDate.minusWeeks(1)
                                "Month" -> displayedDate.minusMonths(1)
                                else -> displayedDate
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryColor, shape = CircleShapePrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            displayedDate = when (currentView) {
                                "Day" -> displayedDate.plusDays(1)
                                "Week" -> displayedDate.plusWeeks(1)
                                "Month" -> displayedDate.plusMonths(1)
                                else -> displayedDate
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryColor, shape = CircleShapePrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
                    }
                }
            }
        }
    )
}

// Day View
@Composable
fun DayView(displayedDate: LocalDate) {
    val hourHeight = 80.dp
    val hours = (0..23).map { h -> if (h == 0) 12 else if (h > 12) h - 12 else h }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60_000)
        }
    }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val totalHeight = hourHeight.value * 24
    val yOffset = (currentTime.hour * 60 + currentTime.minute) * (totalHeight / 1440)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val blueLinePositionPx = with(density) { (yOffset.dp + 8.dp).toPx() }

        // Auto-scroll

        var initialScrollDone by remember { mutableStateOf(false) }

        if (displayedDate == LocalDate.now() && !initialScrollDone) {
            LaunchedEffect(Unit) { // Only run once
                if (scrollState.maxValue > 0) {
                    val targetScroll = (blueLinePositionPx - viewportHeightPx / 2f)
                        .coerceIn(0f, scrollState.maxValue.toFloat())
                    scrollState.scrollTo(targetScroll.toInt())
                }
                initialScrollDone = true
            }
        }

        // Hour labels
        Row(modifier = Modifier.verticalScroll(scrollState)) {
            Column {
                hours.forEach { hourLabel ->
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(hourHeight)
                            .padding(start = 10.dp),
                        contentAlignment = Alignment.TopCenter
                    ) { Text("$hourLabel:00", fontSize = 14.sp) }
                }
            }

            // Hour Boxes
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(all = 10.dp)) {
                    hours.forEach { _ ->
                        Box(modifier = Modifier.height(hourHeight).fillMaxWidth().border(0.5.dp, Color.Gray))
                    }
                }

                // 5 Min Lines
                Canvas(modifier = Modifier.matchParentSize().padding(all = 10.dp)) {
                    val totalHeightPx = size.height
                    val minuteHeightPx = totalHeightPx / 1440f
                    val blue = PrimaryColor.copy(alpha = 0.3f)
                    for (minute in 5 until 1440 step 5) {
                        if (minute % 60 != 0) {
                            val y = minute * minuteHeightPx
                            drawLine(color = blue, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                        }
                    }
                }

                // Current Time Line
                if (displayedDate == LocalDate.now()) {
                    Box(
                        modifier = Modifier
                            .offset(y = yOffset.dp + 8.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(horizontal = 10.dp)
                            .background(PrimaryColor)
                    )
                }
            }
        }
    }
}

// Week View
@Composable
fun WeekView(displayedDate: LocalDate) {
    val hourHeight = 60.dp
    val hours = (0..23).map { h -> if (h == 0) 12 else if (h > 12) h - 12 else h }
    val today = LocalDate.now()
    val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    Column(modifier = Modifier.fillMaxSize().padding(end = 10.dp)) {

        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(60.dp))
            (0..6).forEach { i ->
                val day = startOfWeek.plusDays(i.toLong())
                val isToday = day == today
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), fontSize = 14.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (isToday) PrimaryColor else Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.size(40.dp).background(color = if (isToday) PrimaryColor else Color.Transparent, shape = CircleShapePrimary), contentAlignment = Alignment.Center) {
                        Text(day.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (isToday) Color.White else Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {

            // Hour Labels
            Column {
                hours.forEach { hourLabel ->
                    Box(modifier = Modifier.width(50.dp).height(hourHeight), contentAlignment = Alignment.TopStart) {
                        Text("$hourLabel:00", fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp).offset(y = (-5).dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Hour Boxes
            (0..6).forEach {
                Column(modifier = Modifier.weight(1f)) {
                    hours.forEach { _ ->
                        Box(modifier = Modifier.height(hourHeight).fillMaxWidth().offset(y = 5.dp).border(0.5.dp, Color.Gray))
                    }
                }
            }
        }
    }
}

// Month View
@Composable
fun MonthView(displayedDate: LocalDate, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val firstDayOfMonth = displayedDate.withDayOfMonth(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    var currentDate = firstDayOfMonth.minusDays(startDayOfWeek.toLong())

    // 6-week Grid
    val weeks = List(6) {
        List(7) { currentDate.also { currentDate = currentDate.plusDays(1) } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {

        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Box(modifier = Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 14.sp)
                }
            }
        }

        // Calendar Grid
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val isCurrentMonth = date.month == displayedDate.month
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val backgroundColor = if (isSelected) PrimaryColor else Color.Transparent
                    val textColor = when {
                        isSelected -> Color.White
                        isToday && !isSelected -> PrimaryColor
                        !isCurrentMonth -> Color.Gray
                        else -> Color.Black
                    }
                    val textWeight = if (isSelected || (isToday && !isSelected)) FontWeight.Bold else FontWeight.Normal

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .border(1.dp, Color.Gray)
                            .background(backgroundColor)
                            .clickable { onDateSelected(date) }
                            .padding(5.dp),
                        contentAlignment = Alignment.TopCenter
                    ) { Text(date.dayOfMonth.toString(), fontWeight = textWeight, color = textColor, fontSize = 14.sp) }
                }
            }
        }

        // Selected Date Label
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            modifier = Modifier.padding(top = 16.dp).align(Alignment.Start),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Event List
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 6.dp)) {
            items(15) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, PrimaryColor, RoundedCornerShape(8.dp))
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) { Text("Event ${i + 1}", fontSize = 14.sp) }
            }
        }
    }
}
