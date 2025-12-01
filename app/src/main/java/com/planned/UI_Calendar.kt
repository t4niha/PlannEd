package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/* WEEK START LOGIC */
@RequiresApi(Build.VERSION_CODES.O)
fun getFirstDayOfWeek(): DayOfWeek {
    return if (startWeekOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
}
@RequiresApi(Build.VERSION_CODES.O)
fun getLastDayOfWeek(): DayOfWeek {
    return getFirstDayOfWeek().minus(1)
}

/* CALENDARS */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Calendars(db: AppDatabase) {
    // Global states
    val currentView = currentCalendarView
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) { value = LocalTime.now(); delay(60_000) }
    }
    var dayInitialScrollDone by remember { mutableStateOf(false) }
    var weekInitialScrollDone by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = INITIAL_PAGE, pageCount = { TOTAL_PAGES })
    val coroutineScope = rememberCoroutineScope()
    val displayedDate by remember(pagerState.currentPage, currentView) {
        derivedStateOf {
            when (currentView) {
                "Day" -> today.plusDays((pagerState.currentPage - INITIAL_PAGE).toLong())
                "Week" -> today.plusWeeks((pagerState.currentPage - INITIAL_PAGE).toLong())
                "Month" -> today.plusMonths((pagerState.currentPage - INITIAL_PAGE).toLong())
                else -> today
            }
        }
    }

    // Reset scroll states
    LaunchedEffect(calendarResetTrigger) {
        dayInitialScrollDone = false
        weekInitialScrollDone = false
        selectedDate = today
        pagerState.scrollToPage(INITIAL_PAGE)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(BackgroundColor)
    ) {
        // Date header
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
                        val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(getFirstDayOfWeek()))
                        val endOfWeek = displayedDate.with(TemporalAdjusters.nextOrSame(getLastDayOfWeek()))
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
                textAlign = TextAlign.Center
            )
        }

        // Calendar views
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentView) {
                "Day" -> DayView(displayedDate, pagerState, dayInitialScrollDone, onScrollDone = { dayInitialScrollDone = true }, currentTime)
                "Week" -> WeekView(displayedDate, pagerState, weekInitialScrollDone, onScrollDone = { weekInitialScrollDone = true }, currentTime)
                "Month" -> MonthView(selectedDate, { selectedDate = it }, pagerState)
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(all = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                modifier = Modifier.size(40.dp).background(PrimaryColor, shape = CircleShapePrimary)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous ${currentView.lowercase()}", tint = Color.White) }

            IconButton(
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.size(40.dp).background(PrimaryColor, shape = CircleShapePrimary)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next ${currentView.lowercase()}", tint = Color.White) }
        }
    }
}