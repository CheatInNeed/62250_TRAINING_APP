package com.example.gymlocker.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.dao.WorkoutSummary
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.StatViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class CalendarZoom {
    WEEK,
    MONTH,
    YEAR
}

// PS-120: same shape must be used for Card(shape) + border + metalGloss(shape)
private val HomeCardShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val statViewModel: StatViewModel = viewModel()

    val completedWorkouts by activeWorkoutViewModel
        .completedWorkouts()
        .collectAsState(initial = emptyList())

    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)

    val db = remember { AppDatabase.getDatabase(context.applicationContext) }
    val exerciseLogDao = remember { db.exerciseLogDao() }

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") }

    val today = LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val endOfWeek = startOfWeek.plusDays(6)

    val startInclusive = startOfWeek.atStartOfDay().format(formatter)
    val endInclusive = endOfWeek.atTime(23, 59, 59, 999_000_000).format(formatter)



    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,

        // PS: chrome uses shapes + metalGloss
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = { Text("Workout Feed") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },

        // PS: bottom bar must be surface + metalGloss(BotBarShape)
        bottomBar = {
            Surface(
                modifier = Modifier.metalGloss(BotBarShape),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column {
                    ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                    AppBottomBar(navController)
                }
            }
        }
    ) { innerPadding ->

        if (activeProfileUserId == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Create a profile to get started",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Your profile stores your name, height, weight, and workout summary.\nYou can create one from the Profile page.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { navController.navigate("profile") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("Create Profile") }
                }
            }
            return@Scaffold
        }

        val userId = activeProfileUserId!!

        val workoutsThisWeek by exerciseLogDao
            .observeCompletedWorkoutCountInRangeForUser(
                userId = userId,
                startInclusive = startInclusive,
                endInclusive = endInclusive
            )
            .collectAsState(initial = 0)

        val weeklyVolume by statViewModel
            .weeklyVolumeLast3Months(userId)
            .collectAsState(initial = emptyList())

        val weeklyHours by statViewModel
            .weeklyHoursLast3Months(userId)
            .collectAsState(initial = emptyList())

        val sections = remember(completedWorkouts) {
            completedWorkouts
                .map { ws -> ws to ws.homeSafeLocalDateTime(formatter) }
                .sortedByDescending { (_, dt) -> dt ?: LocalDateTime.MIN }
                .groupBy { (_, dt) -> dt?.toLocalDate() }
                .toSortedMap(compareByDescending { it })
        }

        val workoutDates = remember(sections) {
            sections.keys.filterNotNull().toSet()
        }

        val workoutsPerDay = remember(sections) {
            sections
                .filterKeys { it != null }
                .mapKeys { it.key!! }
                .mapValues { (_, list) -> list.size }
        }

        val listState = rememberLazyListState()

        var calendarZoom by rememberSaveable { mutableStateOf(CalendarZoom.WEEK) }
        var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
        var currentMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
        var yearCursor by rememberSaveable { mutableStateOf(currentMonth.year) }

        // Auto-collapse when scrolling feed
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .map { (idx, off) -> idx > 0 || off > 0 }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    // Only auto-collapse if we're NOT in the "single day filter" view
                    if (isScrolling && calendarZoom != CalendarZoom.MONTH) {
                        calendarZoom = CalendarZoom.WEEK
                    }
                }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ExpandableHomeCalendarHeader(
                    workoutsPerDay = workoutsPerDay,
                    selectedDate = selectedDate,
                    currentMonth = currentMonth,
                    yearCursor = yearCursor,
                    zoom = calendarZoom,
                    onZoomChange = { newZoom ->
                        if (newZoom == CalendarZoom.YEAR) yearCursor = currentMonth.year
                        calendarZoom = newZoom
                    },
                    onMonthChange = { currentMonth = it },
                    onYearChange = { yearCursor = it },
                    onDateSelected = { date ->
                        selectedDate = date
                        currentMonth = YearMonth.from(date)
                    }
                )
            }

            if (sections.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .metalGloss(HomeCardShape),
                        shape = HomeCardShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No completed workouts yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                if (calendarZoom == CalendarZoom.MONTH) {
                    // MONTH: show only selected day
                    val itemsForSelectedDay = sections[selectedDate].orEmpty()

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = homeSectionTitleForDate(selectedDate),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    if (itemsForSelectedDay.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .metalGloss(HomeCardShape),
                                shape = HomeCardShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No completed workouts on this day.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(itemsForSelectedDay, key = { (ws, _) -> ws.workoutId }) { (ws, dt) ->
                            HomeWorkoutHistoryCard(
                                workout = ws,
                                dateTime = dt,
                                onClick = { navController.navigate("workoutDetail/${ws.workoutId}") }
                            )
                        }
                    }

                } else {
                    // WEEK (collapsed) + YEAR modes: show all workouts grouped
                    sections.forEach { (date, itemsForDate) ->

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = homeSectionTitleForDate(date ?: LocalDate.now()),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        items(itemsForDate, key = { (ws, _) -> ws.workoutId }) { (ws, dt) ->
                            HomeWorkoutHistoryCard(
                                workout = ws,
                                dateTime = dt,
                                onClick = { navController.navigate("workoutDetail/${ws.workoutId}") }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

/**
 * Pretty date:
 * Input: "yyyy-MM-dd HH:mm:ss.SSS"
 * Output: "Jan 7 2026"
 */
private fun prettyWorkoutDate(raw: String): String {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)
        LocalDateTime.parse(raw, input).format(output)
    } catch (e: Exception) {
        raw
    }
}

private fun HomeParseSetSummaryLine(line: String): Pair<Int, String>? {
    val regex = Regex("""^\s*(\d+)\s*x\s*(.+?)\s*$""")
    val match = regex.find(line) ?: return null
    val count = match.groupValues[1].toIntOrNull() ?: return null
    val name = match.groupValues[2]
    return count to name
}

private fun WorkoutSummary.homeSafeLocalDateTime(formatter: DateTimeFormatter): LocalDateTime? {
    return try {
        LocalDateTime.parse(this.date, formatter)
    } catch (_: Exception) {
        null
    }
}

private fun homeSectionTitleForDate(date: LocalDate?): String {
    if (date == null) return "Unknown date"

    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> {
            val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(date, today)
            if (daysAgo in 2..6) {
                date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            } else {
                date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            }
        }
    }
}

@Composable
private fun HomeWorkoutHistoryCard(
    workout: WorkoutSummary,
    dateTime: LocalDateTime?,
    onClick: () -> Unit
) {
    val timeText = remember(dateTime) {
        dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
    }

    val exerciseLines = remember(workout.exerciseSetSummary) {
        workout.exerciseSetSummary
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .metalGloss(HomeCardShape),
        shape = HomeCardShape,
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column {
            // Accent strip (kept subtle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.0f))
            )

            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.90f))
                    )
                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = workout.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (exerciseLines.isEmpty()) {
                    Text(
                        text = "${workout.exerciseCount} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val cutoff = 5
                    exerciseLines.take(cutoff).forEach { line ->
                        val parsed = HomeParseSetSummaryLine(line)
                        val count = parsed?.first
                        val exName = parsed?.second

                        val text = if (count != null && exName != null) {
                            val unit = if (count == 1) "set" else "sets"
                            "$count $unit of $exName"
                        } else line

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    if (exerciseLines.size > cutoff) {
                        Text(
                            text = "+${exerciseLines.size - cutoff} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyWorkoutsCircles(
    workoutsPerDay: Map<LocalDate, Int>,
    today: LocalDate = LocalDate.now()
) {
    val days = remember(today) { (6L downTo 0L).map { today.minusDays(it) } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        days.forEach { date ->
            val count = workoutsPerDay[date] ?: 0

            WeekDayCircleColumn(
                dayLabel = dayLetter(date.dayOfWeek),
                numberLabel = date.dayOfMonth.toString(),
                workoutCount = count,
                selected = (date == today),
                onClick = { /* TODO: pick date */ }
            )
        }
    }
}

@Composable
private fun WeekDayCircleColumn(
    dayLabel: String,
    numberLabel: String,
    workoutCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val fillTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)

    val selectedFill = MaterialTheme.colorScheme.primary
    val selectedText = MaterialTheme.colorScheme.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(44.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(6.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        selected -> selectedFill
                        workoutCount > 0 -> fillTint
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .border(1.dp, outline, CircleShape)
        ) {
            Text(
                text = numberLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) selectedText else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(6.dp))
        WorkoutDotsRow(count = workoutCount)
    }
}

private fun dayLetter(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "T"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "S"
}

@Composable
private fun WorkoutDotsRow(count: Int) {
    val shown = minOf(count, 3)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(shown) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun ExpandableHomeCalendarHeader(
    workoutsPerDay: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    yearCursor: Int,
    zoom: CalendarZoom,
    onZoomChange: (CalendarZoom) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onYearChange: (Int) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val weekDays = remember(today) { (6L downTo 0L).map { today.minusDays(it) } } // [today-6 .. today]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(zoom) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (zoom == CalendarZoom.WEEK && dragAmount > 18f) onZoomChange(CalendarZoom.MONTH)
                        if (zoom == CalendarZoom.MONTH && dragAmount < -18f) onZoomChange(CalendarZoom.WEEK)
                        if (zoom == CalendarZoom.YEAR && dragAmount < -18f) onZoomChange(CalendarZoom.MONTH)
                    }
                )
            }
    ) {
        MonthYearSwitchHeader(
            currentMonth = currentMonth,
            yearCursor = yearCursor,
            zoom = zoom,
            onZoomChange = onZoomChange,
            onYearChange = onYearChange
        )

        Spacer(Modifier.height(10.dp))

        AnimatedVisibility(
            visible = (zoom == CalendarZoom.WEEK),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                weekDays.forEach { date ->
                    val count = workoutsPerDay[date] ?: 0
                    WeekDayCircleColumn(
                        dayLabel = dayLetter(date.dayOfWeek),
                        numberLabel = date.dayOfMonth.toString(),
                        workoutCount = count,
                        selected = (zoom == CalendarZoom.MONTH) && date == selectedDate,
                        onClick = {
                            onDateSelected(date)
                            onZoomChange(CalendarZoom.MONTH)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = (zoom == CalendarZoom.MONTH),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev month")
                    }
                    IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }

                HomeCalendarGridCounts(
                    currentMonth = currentMonth,
                    workoutsPerDay = workoutsPerDay,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )

                Spacer(Modifier.height(6.dp))
            }
        }

        AnimatedVisibility(
            visible = (zoom == CalendarZoom.YEAR),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            HomeYearGrid(
                year = yearCursor,
                workoutsPerDay = workoutsPerDay,
                onPrevYear = { onYearChange(yearCursor - 1) },
                onNextYear = { onYearChange(yearCursor + 1) },
                onMonthSelected = { ym ->
                    onMonthChange(ym)
                    onDateSelected(ym.atDay(1))
                    onZoomChange(CalendarZoom.MONTH)
                }
            )
        }
    }
}

@Composable
private fun HomeCalendarGridCounts(
    currentMonth: YearMonth,
    workoutsPerDay: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        val totalCells = ((firstDayOfMonth + daysInMonth + 6) / 7) * 7

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(240.dp),
            userScrollEnabled = false
        ) {
            items(totalCells) { index ->
                val dayOfMonth = index - firstDayOfMonth + 1

                if (dayOfMonth in 1..daysInMonth) {
                    val date = currentMonth.atDay(dayOfMonth)
                    val count = workoutsPerDay[date] ?: 0
                    val hasWorkout = count > 0
                    val isSelected = date == selectedDate

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    hasWorkout -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onBackground
                                }
                            )

                            if (count > 0) {
                                Spacer(Modifier.height(4.dp))
                                WorkoutDotsRow(count = count.coerceAtMost(3))
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeYearGrid(
    year: Int,
    workoutsPerDay: Map<LocalDate, Int>,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit
) {
    val months = remember(year) { (1..12).map { YearMonth.of(year, it) } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevYear) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev year")
            }
            IconButton(onClick = onNextYear) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next year")
            }
        }

        Spacer(Modifier.height(8.dp))

        val rows = remember(year) { months.chunked(3) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            rows.forEach { rowMonths ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    rowMonths.forEach { ym ->
                        MonthMini(
                            month = ym,
                            workoutsPerDay = workoutsPerDay,
                            onClick = { onMonthSelected(ym) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    repeat(3 - rowMonths.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MiniMonthCalendar(
    month: YearMonth,
    workoutsPerDay: Map<LocalDate, Int>,
) {
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = (firstDay.dayOfWeek.value - 1).coerceAtLeast(0)

    val workoutColor = MaterialTheme.colorScheme.primary
    val dayTextStyle = MaterialTheme.typography.labelSmall.copy(
        lineHeight = MaterialTheme.typography.labelSmall.fontSize,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(86.dp),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(startOffset) { Box(modifier = Modifier.size(12.dp)) }

        items(daysInMonth) { i ->
            val day = i + 1
            val date = month.atDay(day)
            val count = workoutsPerDay[date] ?: 0
            val hasWorkout = count > 0

            Box(
                modifier = Modifier.size(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasWorkout) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(workoutColor, CircleShape)
                    )
                    Text(
                        text = day.toString(),
                        style = dayTextStyle,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = day.toString(),
                        style = dayTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val totalCells = startOffset + daysInMonth
        val rest = (7 - (totalCells % 7)) % 7
        items(rest) { Box(modifier = Modifier.size(12.dp)) }
    }
}

@Composable
private fun MonthMini(
    month: YearMonth,
    workoutsPerDay: Map<LocalDate, Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Text(
            text = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))

        MiniMonthCalendar(
            month = month,
            workoutsPerDay = workoutsPerDay
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MonthYearSwitchHeader(
    currentMonth: YearMonth,
    yearCursor: Int,
    zoom: CalendarZoom,
    onZoomChange: (CalendarZoom) -> Unit,
    onYearChange: (Int) -> Unit
) {
    val monthText =
        "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"

    val yearTextForLeft = currentMonth.year.toString()
    val yearTextForCenter = yearCursor.toString()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp),
    ) {
        AnimatedContent(
            targetState = zoom,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (
                    (initialState == CalendarZoom.MONTH && targetState == CalendarZoom.YEAR) ||
                    (initialState == CalendarZoom.WEEK && targetState == CalendarZoom.YEAR)
                ) {
                    (slideInHorizontally { w -> -w / 2 } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> w / 2 } + fadeOut())
                } else if (
                    (initialState == CalendarZoom.YEAR && targetState == CalendarZoom.MONTH) ||
                    (initialState == CalendarZoom.YEAR && targetState == CalendarZoom.WEEK)
                ) {
                    (slideInHorizontally { w -> w / 2 } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> -w / 2 } + fadeOut())
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            label = "MonthYearSwitch"
        ) { state ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (state == CalendarZoom.WEEK || state == CalendarZoom.MONTH) {
                    Text(
                        text = yearTextForLeft,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clickable {
                                onYearChange(currentMonth.year)
                                onZoomChange(CalendarZoom.YEAR)
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }

                when (state) {
                    CalendarZoom.WEEK -> {
                        Text(
                            text = monthText,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clickable { onZoomChange(CalendarZoom.MONTH) }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    CalendarZoom.MONTH -> {
                        Text(
                            text = monthText,
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    CalendarZoom.YEAR -> {
                        Text(
                            text = yearTextForCenter,
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = monthText,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable { onZoomChange(CalendarZoom.MONTH) }
                                .padding(start = 4.dp, end = 44.dp, top = 6.dp, bottom = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = {
                when (zoom) {
                    CalendarZoom.WEEK -> onZoomChange(CalendarZoom.MONTH)
                    CalendarZoom.MONTH -> onZoomChange(CalendarZoom.WEEK)
                    CalendarZoom.YEAR -> onZoomChange(CalendarZoom.WEEK)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = if (zoom == CalendarZoom.WEEK)
                    Icons.Default.KeyboardArrowDown
                else
                    Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
