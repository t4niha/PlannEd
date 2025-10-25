package com.planned

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planned.ui.theme.PlanEdTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*

// UI constants
private val PrimaryColor = Color(0xFF1976D2)
private val CircleShapePrimary = CircleShape

// Calendar constants
private const val INITIAL_PAGE = 10000
private const val TOTAL_PAGES = 20000

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setContent {
                PlanEdTheme {
                    HomeScreen()
                }
            }
        } else {
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("This app requires Android 8.0 or higher")
                }
            }
        }
    }
}

// Home screen
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen() {
    var currentView by remember { mutableStateOf("Day") }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    // Shared current time state using produceState
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000)
        }
    }

    // Scroll state
    var dayInitialScrollDone by remember { mutableStateOf(false) }
    var weekInitialScrollDone by remember { mutableStateOf(false) }

    // Pager state
    val pagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { TOTAL_PAGES }
    )
    val coroutineScope = rememberCoroutineScope()

    // Displayed date
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

    val selectorButtonWidth = 90.dp
    val selectorButtonHeight = 34.dp
    val selectorButtonCorner = 10.dp

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    // Menu button
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.size(32.dp))
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View selector buttons
                        Row(
                            modifier = Modifier.weight(1.8f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            listOf("Day", "Week", "Month").forEach { view ->
                                val selected = view == currentView
                                Button(
                                    onClick = {
                                        currentView = view
                                        selectedDate = today
                                        if (view == "Day") dayInitialScrollDone = false
                                        if (view == "Week") weekInitialScrollDone = false
                                        coroutineScope.launch { pagerState.scrollToPage(INITIAL_PAGE) }
                                    },
                                    shape = RoundedCornerShape(selectorButtonCorner),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) PrimaryColor else Color.LightGray,
                                        contentColor = if (selected) Color.White else Color.Black
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .width(selectorButtonWidth)
                                        .height(selectorButtonHeight)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = view,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Create button
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.Add, contentDescription = "Create", modifier = Modifier.size(32.dp))
                        }
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
                        textAlign = TextAlign.Center
                    )
                }

                // Calendar views
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentView) {
                        "Day" -> DayView(
                            displayedDate,
                            pagerState,
                            dayInitialScrollDone,
                            onScrollDone = { dayInitialScrollDone = true },
                            currentTime = currentTime
                        )
                        "Week" -> WeekView(
                            displayedDate,
                            pagerState,
                            weekInitialScrollDone,
                            onScrollDone = { weekInitialScrollDone = true },
                            currentTime = currentTime
                        )
                        "Month" -> MonthView(
                            selectedDate,
                            { selectedDate = it },
                            pagerState
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.size(40.dp).background(PrimaryColor, shape = CircleShapePrimary)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous ${currentView.lowercase()}",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.size(40.dp).background(PrimaryColor, shape = CircleShapePrimary)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next ${currentView.lowercase()}",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    )
}

// Day view
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayView(
    displayedDate: LocalDate,
    pagerState: androidx.compose.foundation.pager.PagerState,
    initialScrollDone: Boolean,
    onScrollDone: () -> Unit,
    currentTime: LocalTime
) {
    val hourHeight = 80.dp
    val hours = (0..23).toList()

    // Auto scroll
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val totalHeight = hourHeight.value * 24
    val yOffset = (currentTime.hour * 60 + currentTime.minute) * (totalHeight / 1440)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val blueLinePositionPx = with(density) { (yOffset.dp + 8.dp).toPx() }

        LaunchedEffect(displayedDate, initialScrollDone, scrollState.maxValue) {
            if (displayedDate == LocalDate.now() && !initialScrollDone && scrollState.maxValue > 0) {
                val targetScroll = (blueLinePositionPx - viewportHeightPx / 2f)
                    .coerceIn(0f, scrollState.maxValue.toFloat())
                scrollState.scrollTo(targetScroll.toInt())
                onScrollDone()
            }
        }

        Row {
            // Hour labels
            Column(modifier = Modifier.verticalScroll(scrollState).padding(top = 10.dp)) {
                hours.forEach { hour ->
                    val displayHour = "${if (hour % 12 == 0) 12 else hour % 12} ${if (hour < 12) "AM" else "PM"}"
                    Box(
                        modifier = Modifier.width(60.dp).height(hourHeight).padding(start = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(displayHour, fontSize = 14.sp, color = Color.Black, modifier = Modifier.offset(y = (-7).dp))
                    }
                }
            }

            // Hour grid
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                val pageDate = LocalDate.now().plusDays((page - INITIAL_PAGE).toLong())
                Box(modifier = Modifier.verticalScroll(scrollState).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
                        hours.forEach { _ ->
                            Box(modifier = Modifier.height(hourHeight).fillMaxWidth().border(0.5.dp, Color.LightGray))
                        }
                    }

                    // Current time line
                    if (pageDate == LocalDate.now()) {
                        Box(
                            modifier = Modifier
                                .offset(y = yOffset.dp + 10.dp)
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
}

// Week view
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeekView(
    displayedDate: LocalDate,
    pagerState: androidx.compose.foundation.pager.PagerState,
    initialScrollDone: Boolean,
    onScrollDone: () -> Unit,
    currentTime: LocalTime
) {
    val hourHeight = 80.dp
    val hours = (0..23).toList()
    val today = LocalDate.now()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Header height
    var headerHeightPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalHeight = hourHeight.value * 24
        val yOffset = (currentTime.hour * 60 + currentTime.minute) * (totalHeight / 1440)
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val linePositionPx = with(density) { yOffset.dp.toPx() }

        // Auto scroll
        LaunchedEffect(displayedDate, initialScrollDone, headerHeightPx, scrollState.maxValue) {
            if (!initialScrollDone && headerHeightPx > 0 && scrollState.maxValue > 0) {
                val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val endOfWeek = displayedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                if (!today.isBefore(startOfWeek) && !today.isAfter(endOfWeek)) {
                    val targetScroll = (linePositionPx - viewportHeightPx / 2f + headerHeightPx)
                        .coerceIn(0f, scrollState.maxValue.toFloat())
                    scrollState.scrollTo(targetScroll.toInt())
                    onScrollDone()
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Weekday headers
            val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .onGloballyPositioned { coords -> headerHeightPx = coords.size.height.toFloat() }
            ) {
                Spacer(modifier = Modifier.width(60.dp))
                (0..6).forEach { i ->
                    val day = startOfWeek.plusDays(i.toLong())
                    val isToday = day == today
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            fontSize = 14.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) PrimaryColor else Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.size(40.dp)
                                .background(if (isToday) PrimaryColor else Color.Transparent, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.dayOfMonth.toString(),
                                fontSize = 14.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hour labels
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.width(60.dp).verticalScroll(scrollState).padding(top = 10.dp)
                ) {
                    hours.forEach { hour ->
                        val displayHour = "${if (hour % 12 == 0) 12 else hour % 12} ${if (hour < 12) "AM" else "PM"}"
                        Box(
                            modifier = Modifier.height(hourHeight).fillMaxWidth().padding(start = 8.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(displayHour, fontSize = 14.sp, color = Color.Black, modifier = Modifier.offset(y = (-7).dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Hour grid
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().padding(end = 10.dp)
                ) { page ->
                    val pageDate = LocalDate.now().plusWeeks((page - INITIAL_PAGE).toLong())
                    val pageStartOfWeek = pageDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    val todayIndex = today.dayOfWeek.value % 7

                    Row(modifier = Modifier.verticalScroll(scrollState).fillMaxWidth().padding(top = 10.dp)) {
                        (0..6).forEachIndexed { index, _ ->
                            Box(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    hours.forEach { _ ->
                                        Box(
                                            modifier = Modifier.height(hourHeight).fillMaxWidth().border(0.5.dp, Color.LightGray)
                                        )
                                    }
                                }

                                // Current time line
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    val minuteHeightPx = size.height / 1440f
                                    if (index == todayIndex && pageStartOfWeek.plusDays(index.toLong()) == today) {
                                        val lineY = (currentTime.hour * 60 + currentTime.minute) * minuteHeightPx
                                        drawLine(
                                            color = PrimaryColor,
                                            start = Offset(0f, lineY),
                                            end = Offset(size.width, lineY),
                                            strokeWidth = 4.dp.toPx()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Month view
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun MonthView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState
) {
    val today = LocalDate.now()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        // Weekday labels
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Box(modifier = Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 14.sp)
                }
            }
        }

        // Month grid
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 4.dp)) {
                val pageDate = LocalDate.now().plusMonths((page - INITIAL_PAGE).toLong())
                val firstDayOfMonth = pageDate.withDayOfMonth(1)
                val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
                var currentDate = firstDayOfMonth.minusDays(startDayOfWeek.toLong())

                val weeks = List(6) {
                    List(7) { currentDate.also { currentDate = currentDate.plusDays(1) } }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    weeks.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            week.forEach { date ->
                                val isCurrentMonth = date.month == pageDate.month
                                val isSelected = date == selectedDate
                                val isToday = date == today
                                val backgroundColor = if (isSelected) PrimaryColor else Color.Transparent
                                val textColor = when {
                                    isSelected -> Color.White
                                    isToday && !isSelected -> PrimaryColor
                                    !isCurrentMonth -> Color.Gray
                                    else -> Color.Black
                                }
                                val textWeight = if (isSelected || (isToday)) FontWeight.Bold else FontWeight.Normal

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(1.dp, Color.LightGray)
                                        .background(backgroundColor)
                                        .clickable {
                                            onDateSelected(date)
                                            coroutineScope.launch { listState.scrollToItem(0) }
                                        }
                                        .padding(5.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        date.dayOfMonth.toString(),
                                        fontWeight = textWeight,
                                        color = textColor,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected date
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            modifier = Modifier.padding(top = 16.dp).align(Alignment.Start),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Events
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 6.dp)
        ) {
            items(15) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, PrimaryColor, RoundedCornerShape(8.dp))
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Event ${i + 1}", fontSize = 14.sp)
                }
            }
        }
    }
}