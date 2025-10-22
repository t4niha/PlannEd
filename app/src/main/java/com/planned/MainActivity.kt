package com.planned

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planned.ui.theme.PlanEdTheme
import kotlinx.coroutines.delay
import java.time.*
import java.time.temporal.TemporalAdjusters
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.time.DayOfWeek
import java.time.LocalDate
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
                    .padding(8.dp)
            ) {
                // Slider Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("Day", "Week", "Month").forEach { view ->
                        val selected = view == currentView
                        Button(
                            onClick = { currentView = view },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF1976D2) else Color.LightGray,
                                contentColor = if (selected) Color.White else Color.Black
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(view)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Header for current date / month / week
                Text(
                    text = when (currentView) {
                        "Day" -> displayedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                        "Week" -> {
                            val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                            val endOfWeek = displayedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                            val startMonth = startOfWeek.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            val endMonth = endOfWeek.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            if (startOfWeek.month == endOfWeek.month) {
                                "$startMonth ${startOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth}, ${startOfWeek.year}"
                            } else {
                                "$startMonth ${startOfWeek.dayOfMonth} - $endMonth ${endOfWeek.dayOfMonth}, ${startOfWeek.year}"
                            }
                        }
                        "Month" -> displayedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                        else -> ""
                    },
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Calendar container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFECEFF1))
                        .padding(4.dp)
                ) {
                    when (currentView) {
                        "Day" -> DayView(displayedDate)
                        "Week" -> WeekView(displayedDate)
                        "Month" -> MonthView(displayedDate)
                    }
                }

                // Bottom arrows
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
                    }) { Text("<") }

                    Button(onClick = {
                        displayedDate = when (currentView) {
                            "Day" -> displayedDate.plusDays(1)
                            "Week" -> displayedDate.plusWeeks(1)
                            "Month" -> displayedDate.plusMonths(1)
                            else -> displayedDate
                        }
                    }) { Text(">") }
                }
            }
        }
    )
}

// ---------------- Day View ----------------
@Composable
fun DayView(displayedDate: LocalDate) {
    val hours = (0..23).map { hour ->
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$displayHour:00"
    }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60_000)
        }
    }

    Row(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Column {
            hours.forEach { hourLabel ->
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(50.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(hourLabel, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp * 24)
                .border(1.dp, Color.Gray)
        ) {
            Column {
                (0..23).forEach { _ ->
                    Divider(color = Color.Gray, thickness = 0.5.dp)
                }
            }

            val totalHeight = 60f * 24
            val yOffset = (currentTime.hour * 60 + currentTime.minute) * (totalHeight / 1440)
            Box(
                modifier = Modifier
                    .offset(y = yOffset.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.Red)
            )
        }
    }
}

// ---------------- Week View ----------------
@Composable
fun WeekView(displayedDate: LocalDate) {
    val hourHeight = 80.dp // adjust this to make hours taller or shorter
    val hours = (0..23).map { hour ->
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$displayHour:00"
    }

    val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        // Hour labels column
        Column(modifier = Modifier.width(50.dp)) {
            hours.forEach { hourLabel ->
                Box(
                    modifier = Modifier
                        .height(hourHeight),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(hourLabel, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Each day column
        (0..6).forEach { i ->
            val day = startOfWeek.plusDays(i.toLong())
            Column {
                // Day header
                Text(
                    text = day.dayOfMonth.toString(),
                    modifier = Modifier.height(30.dp),
                    fontSize = 14.sp
                )

                // Hour blocks
                hours.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .height(hourHeight)
                            .width(80.dp)
                            .border(0.5.dp, Color.Gray)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
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
            else if (dayCounter <= daysInMonth) {
                week.add(dayCounter)
                dayCounter++
            } else week.add(null)
        }
        weeks.add(week)
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Row {
            listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 14.sp)
                }
            }
        }

        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(0.5.dp, Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            Text(
                                text = day.toString(),
                                color = if (day == today.dayOfMonth && displayedDate.month == today.month) Color.Blue else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
