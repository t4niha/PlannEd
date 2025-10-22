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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planned.ui.theme.PlanEdTheme
import kotlinx.coroutines.delay
import java.time.*
import java.time.temporal.TemporalAdjusters
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var currentView by remember { mutableStateOf("Day") }
    var displayedDate by remember { mutableStateOf(LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PlannEd", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: open drawer */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: account page */ }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
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
                // View selector buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val buttonWidth = 110.dp // set your desired fixed width
                    listOf("Day", "Week", "Month").forEach { view ->
                        val selected = view == currentView
                        Button(
                            onClick = { currentView = view },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF1976D2) else Color.LightGray,
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

                // Header for date/week/month
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .padding(top = 5.dp)
                        .padding(horizontal = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (currentView) {
                            "Day" -> displayedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                            "Week" -> {
                                val startOfWeek =
                                    displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                                val endOfWeek =
                                    displayedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

                                val startMonth = startOfWeek.format(DateTimeFormatter.ofPattern("MMM"))
                                val endMonth = endOfWeek.format(DateTimeFormatter.ofPattern("MMM"))

                                if (startOfWeek.month == endOfWeek.month) {
                                    "$startMonth ${startOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth}, ${endOfWeek.year}"
                                } else {
                                    "$startMonth ${startOfWeek.dayOfMonth} - $endMonth ${endOfWeek.dayOfMonth}, ${endOfWeek.year}"
                                }
                            }

                            "Month" -> displayedDate.format(DateTimeFormatter.ofPattern("MMMM, yyyy"))
                            else -> ""
                        },
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                // Calendar container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when (currentView) {
                        "Day" -> DayView(displayedDate)
                        "Week" -> WeekView(displayedDate)
                        "Month" -> MonthView(displayedDate)
                    }
                }

                // Bottom navigation arrows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        displayedDate = when (currentView) {
                            "Day" -> displayedDate.minusDays(1)
                            "Week" -> displayedDate.minusWeeks(1)
                            "Month" -> displayedDate.minusMonths(1)
                            else -> displayedDate
                        }
                    }) { Text("<", fontSize = 14.sp) }

                    Button(onClick = {
                        displayedDate = when (currentView) {
                            "Day" -> displayedDate.plusDays(1)
                            "Week" -> displayedDate.plusWeeks(1)
                            "Month" -> displayedDate.plusMonths(1)
                            else -> displayedDate
                        }
                    }) { Text(">", fontSize = 14.sp) }
                }
            }
        }
    )
}

// ---------------- Day View ----------------
@Composable
fun DayView(displayedDate: LocalDate) {
    val hourHeight = 80.dp
    val hours = (0..23).map { hour ->
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        "$displayHour:00"
    }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60_000)
        }
    }

    Row(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // Hour labels column
        Column(
            modifier = Modifier
                .width(60.dp)
                .padding(all = 10.dp)
        ) {
            hours.forEach { hourLabel ->
                Box(
                    modifier = Modifier.height(hourHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(hourLabel, fontSize = 14.sp)
                }
            }
        }

        // Day column
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp)
                .padding(top=11.5.dp)
        ) {
            Column(modifier = Modifier.padding(all = 10.dp)) {
                hours.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth()
                            .border(0.5.dp, Color.Gray)
                    )
                }
            }

            // Current time line
            val totalHeight = hourHeight.value * 24
            val yOffset = (currentTime.hour * 60 + currentTime.minute) * (totalHeight / 1440)
            Box(
                modifier = Modifier
                    .offset(y = yOffset.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(end=10.dp)
                    .background(Color(0xFF1976D2))
            )
        }
    }
}

// ---------------- Week View ----------------
@Composable
fun WeekView(displayedDate: LocalDate) {
    val hourHeight = 80.dp
    val hours = (0..23).map { hour ->
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        "$displayHour:00"
    }

    val today = LocalDate.now()
    val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
    ) {
        // Hour column
        Column(modifier = Modifier.width(60.dp)) {
            Spacer(modifier = Modifier.height(66.5.dp))
            hours.forEach { hourLabel ->
                Box(
                    modifier = Modifier
                        .height(hourHeight)
                        .padding(all = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(hourLabel, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        (0..6).forEach { i ->
            val day = startOfWeek.plusDays(i.toLong())
            val isToday = day == today

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                // Day-of-week text (no circle)
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Day-of-month circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isToday) Color(0xFF1976D2) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day.dayOfMonth.toString(),
                        fontSize = 14.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) Color.White else Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hour blocks
                hours.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .height(hourHeight)
                            .width(80.dp)
                            .border(0.5.dp, Color.Gray),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(20.dp))
    }
}

// ---------------- Month View ----------------
@Composable
fun MonthView(displayedDate: LocalDate) {
    val today = LocalDate.now()
    val firstDayOfMonth = displayedDate.withDayOfMonth(1)
    val daysInMonth = displayedDate.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val weeks = mutableListOf<List<Int?>>()
    var dayCounter = 1
    while (dayCounter <= daysInMonth) {
        val week = mutableListOf<Int?>()
        for (i in 0..6) {
            if (weeks.isEmpty() && i < startDayOfWeek) week.add(null)
            else if (dayCounter <= daysInMonth) week.add(dayCounter++)
            else week.add(null)
        }
        weeks.add(week)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(all=10.dp)
            .padding(horizontal=10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { day ->
                Box(
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentAlignment = Alignment.TopCenter
                ) { Text(day, fontSize = 14.sp) }
            }
        }

        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val isToday = day == today.dayOfMonth && displayedDate.month == today.month
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                            .border(0.5.dp, Color.Gray)
                            .background(if (isToday) Color(0xFF1976D2) else Color.Transparent)
                            .padding(all = 5.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (day != null) {
                            Text(
                                text = day.toString(),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
