package com.planned

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/* DAY VIEW */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayView(
    displayedDate: LocalDate,
    pagerState: androidx.compose.foundation.pager.PagerState,
    initialScrollDone: Boolean,
    onScrollDone: () -> Unit,
    currentTime: LocalTime
) {
    val hours = (0..23).toList()

    // Auto scroll
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val totalHeight = HourHeight.value * 24
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
                        modifier = Modifier.width(60.dp).height(HourHeight).padding(start = 8.dp),
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
                            Box(modifier = Modifier.height(HourHeight).fillMaxWidth().border(0.5.dp, Color.LightGray))
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

/* WEEK VIEW */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeekView(
    displayedDate: LocalDate,
    pagerState: androidx.compose.foundation.pager.PagerState,
    initialScrollDone: Boolean,
    onScrollDone: () -> Unit,
    currentTime: LocalTime
) {
    val hours = (0..23).toList()
    val today = LocalDate.now()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Header height
    var headerHeightPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalHeight = HourHeight.value * 24
        val yOffset = (currentTime.hour * 60 + currentTime.minute) * (totalHeight / 1440)
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val linePositionPx = with(density) { yOffset.dp.toPx() }

        // Auto scroll
        LaunchedEffect(displayedDate, initialScrollDone, headerHeightPx, scrollState.maxValue) {
            if (!initialScrollDone && headerHeightPx > 0 && scrollState.maxValue > 0) {
                val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(getFirstDayOfWeek()))
                val endOfWeek = displayedDate.with(TemporalAdjusters.nextOrSame(getLastDayOfWeek()))
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
            val startOfWeek = displayedDate.with(TemporalAdjusters.previousOrSame(getFirstDayOfWeek()))
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
                            modifier = Modifier.height(HourHeight).fillMaxWidth().padding(start = 8.dp),
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
                    val pageStartOfWeek = pageDate.with(TemporalAdjusters.previousOrSame(getFirstDayOfWeek()))
                    val todayIndex = ((today.dayOfWeek.value - getFirstDayOfWeek().value + 7) % 7)

                    Row(modifier = Modifier.verticalScroll(scrollState).fillMaxWidth().padding(top = 10.dp)) {
                        (0..6).forEachIndexed { index, _ ->
                            Box(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    hours.forEach { _ ->
                                        Box(
                                            modifier = Modifier.height(HourHeight).fillMaxWidth().border(0.5.dp, Color.LightGray)
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

/* MONTH VIEW */
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
            val daysOfWeek = (0..6).map { i ->
                DayOfWeek.of(((getFirstDayOfWeek().value - 1 + i) % 7 + 1))
            }
            daysOfWeek.forEach { day ->
                Box(modifier = Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                    Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), fontSize = 14.sp)
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
                val daysToSubtract = (firstDayOfMonth.dayOfWeek.value - getFirstDayOfWeek().value + 7) % 7
                var currentDate = firstDayOfMonth.minusDays(daysToSubtract.toLong())

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
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Event ${i + 1}", fontSize = 14.sp)
                }
            }
        }
    }
}