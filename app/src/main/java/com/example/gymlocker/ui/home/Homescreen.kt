package com.example.gymlocker.ui.home

import android.R.style.Theme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.dao.WorkoutSummary
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.AppTheme
import com.example.gymlocker.data.repo.SettingsRepository
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.components.MuscleGroupDistributionChart
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.StatViewModel
import com.example.gymlocker.viewmodel.StatsRange
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.IconButton
import com.example.gymlocker.ui.history.HistoryViewMode
import com.example.gymlocker.ui.history.WorkoutCalendar
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import java.time.temporal.TemporalAdjusters
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private enum class CalendarZoom {
    WEEK,   // din “collapsed” uge-strip
    MONTH,  // din “expanded” måned-grid
    YEAR    // ny: 12 måneder
}

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

    val lastWorkoutLabel by activeWorkoutViewModel
        .lastWorkoutLabel()
        .collectAsState(initial = "Finder seneste workout…")

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
        topBar = {
            TopAppBar(
                title = { Text("Workout Feed") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column {
                ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                AppBottomBar(navController)
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

        val statsRange by statViewModel.statsRange.collectAsState()

        val distribution by statViewModel
            .muscleGroupDistribution(userId)
            .collectAsState(initial = emptyList())

        var historyViewMode by remember {
            mutableStateOf(HistoryViewMode.LIST)
        }

        val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") }


        val timeFormatter = remember {
            DateTimeFormatter.ofPattern("HH:mm")
        }

        val sections = remember(completedWorkouts) {
            completedWorkouts
                .map { ws -> ws to ws.homeSafeLocalDateTime(formatter) }
                .sortedByDescending { (_, dt) -> dt ?: LocalDateTime.MIN }
                .groupBy { (_, dt) -> dt?.toLocalDate() }
                .toSortedMap(compareByDescending { it })
        }

        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)

        val workoutDatesThisWeek = remember(sections, weekStart, weekEnd) {
            sections.keys
                .filterNotNull()
                .filter { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
                .toSet()
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

        // Auto-collapse når man scroller i feedet
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .map { (idx, off) -> idx > 0 || off > 0 }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    if (isScrolling) calendarZoom = CalendarZoom.WEEK
                }
        }

        LazyColumn(
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
                    zoom = calendarZoom,
                    onZoomChange = { calendarZoom = it },
                    onMonthChange = { currentMonth = it },
                    onDateSelected = { date ->
                        selectedDate = date
                        currentMonth = YearMonth.from(date)
                    }
                )
            }

            if (sections.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
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
                    // MONTH: vis KUN selected day (som før)
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
                            Card(modifier = Modifier.fillMaxWidth()) {
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
                    // ✅ COLLAPSED: vis ALLE workouts (alle sektioner)
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
            }}
    }
}

@Composable
private fun SegmentedToggle(
    leftText: String,
    rightText: String,
    isLeftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    selectedContainerColor: androidx.compose.ui.graphics.Color,
    selectedContentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val selectedColors = ButtonDefaults.buttonColors(
        containerColor = selectedContainerColor,
        contentColor = selectedContentColor
    )

    val unselectedColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onLeftClick,
            shape = RoundedCornerShape(999.dp),
            colors = if (isLeftSelected) selectedColors else unselectedColors,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.wrapContentWidth()
        ) { Text(leftText) }

        Button(
            onClick = onRightClick,
            shape = RoundedCornerShape(999.dp),
            colors = if (!isLeftSelected) selectedColors else unselectedColors,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.wrapContentWidth()
        ) { Text(rightText) }
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

@Composable
fun CompletedWorkoutsCard(
    workouts: List<WorkoutSummary>,
    onViewHistoryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Completed Workouts",
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (workouts.isEmpty()) {
                Text(
                    "No completed workouts yet.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                )
            } else {
                workouts.take(5).forEach { w ->
                    val prettyDate = prettyWorkoutDate(w.date)
                    Text(
                        "• ${w.name} - $prettyDate - ${w.exerciseCount} exercises",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Workout History",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSwitcherCard(
    currentTheme: AppTheme,
    forceDarkMode: Boolean,
    onThemeSelected: (AppTheme) -> Unit,
    onForceDarkChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Force dark mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Overrides system theme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = forceDarkMode,
                onCheckedChange = onForceDarkChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
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

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column {

            // --- Accent strip (brand feel uden at "male" hele kortet) ---
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
                    // lille badge-dot (primary) -> “alive” feel
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

                // resten af dit content uændret
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
    // Rolling 7 dage: [today-6, ..., today]
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
                onClick = { /* TODO: vælg dato */ }
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
private fun WeekDayCircle(
    trained: Boolean,
    isToday: Boolean
) {
    val fill = MaterialTheme.colorScheme.primary
    val emptyFill = MaterialTheme.colorScheme.surface

    val outline = when {
        trained -> fill
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(if (trained) fill else emptyFill)
            .border(1.dp, outline, CircleShape)
    )
}

@Composable
private fun WorkoutDotsRow(count: Int) {
    // cap så UI ikke eksploderer, hvis en dag har fx 10 workouts
    val shown = minOf(count, 4)

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

        // hvis der er flere end 4: lille "+N"
        if (count > 4) {
            Spacer(Modifier.width(2.dp))
            Text(
                text = "+${count - 4}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpandableHomeCalendarHeader(
    workoutsPerDay: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    zoom: CalendarZoom,
    onZoomChange: (CalendarZoom) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    // Uge baseret på selectedDate (så den “følger” hvad man klikker)
    val weekStart = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val today = remember { LocalDate.now() }
    val weekDays = remember(today) { (6L downTo 0L).map { today.minusDays(it) } } // [today-6 .. today]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(zoom) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (zoom == CalendarZoom.WEEK && dragAmount > 18f) {
                            onZoomChange(CalendarZoom.MONTH)
                        }
                        if (zoom == CalendarZoom.MONTH && dragAmount < -18f) {
                            onZoomChange(CalendarZoom.WEEK)
                        }
                        if (zoom == CalendarZoom.YEAR && dragAmount < -18f) {
                            onZoomChange(CalendarZoom.MONTH)
                        }
                    }
                )
            }
    ) {
        // Month title row (klik for expand/collapse)
        MonthYearSwitchHeader(
            currentMonth = currentMonth,
            zoom = zoom,
            onZoomChange = onZoomChange
        )


        Spacer(Modifier.height(10.dp))

        // Week strip (altid synlig)
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

        // Month grid (kun når expanded)
        AnimatedVisibility(
            visible = (zoom == CalendarZoom.MONTH),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))

                // Month nav (chevrons)
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
//                    Text(
//                        text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold
//                    )
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
                year = currentMonth.year,
                workoutsPerDay = workoutsPerDay,
                onPrevYear = { onMonthChange(currentMonth.minusYears(1)) },
                onNextYear = { onMonthChange(currentMonth.plusYears(1)) },
                onMonthSelected = { ym ->
                    onMonthChange(ym)
                    // valgfrit: behold selectedDate hvis den ligger i samme måned,
                    // ellers hop til 1. i måneden:
                    onDateSelected(ym.atDay(1))
                    onZoomChange(CalendarZoom.MONTH) // zoom ind på valgt måned
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

                            // lille dot-row under tallet (som jeres uge-strip)
                            if (count > 0) {
                                Spacer(Modifier.height(4.dp))
                                WorkoutDotsRow(count = count)
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

        // Year nav (chevrons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevYear) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev year")
            }

            // (valgfrit) hvis du vil vise årstal her også (du har det måske i titel allerede)
            // Text(text = year.toString(), style = MaterialTheme.typography.titleMedium)

            IconButton(onClick = onNextYear) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next year")
            }
        }

        Spacer(Modifier.height(8.dp))

        // 12 måneder -> 4 rækker á 3 kolonner
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

                    // safety hvis der nogensinde er en række med < 3
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

    // Mandag=1 ... søndag=7  (justér hvis du vil starte med søndag)
    val startOffset = (firstDay.dayOfWeek.value - 1).coerceAtLeast(0)
    val totalCells = startOffset + daysInMonth

    // “Apple blå” (du kan også bare bruge MaterialTheme.colorScheme.primary)
    val workoutColor = MaterialTheme.colorScheme.primary
    val dayTextStyle = MaterialTheme.typography.labelSmall.copy(
        lineHeight = MaterialTheme.typography.labelSmall.fontSize,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    // 7 kolonner, små tal
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(86.dp),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // tomme celler før 1. dag
        items(startOffset) {
            Box(modifier = Modifier.size(12.dp))
        }

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

        // (valgfrit) fyld resten ud så grid ikke "hopper"
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
    zoom: CalendarZoom,
    onZoomChange: (CalendarZoom) -> Unit
) {
    val yearText = currentMonth.year.toString()
    val monthText = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp),
    ) {
        // ---- SWITCH-AREA (venstre/center/højre labels) ----
        AnimatedContent(
            targetState = zoom,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp),
            transitionSpec = {
                if (initialState == CalendarZoom.MONTH && targetState == CalendarZoom.YEAR) {
                    (slideInHorizontally { w -> -w / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> w / 3 } + fadeOut())
                } else if (initialState == CalendarZoom.YEAR && targetState == CalendarZoom.MONTH) {
                    (slideInHorizontally { w -> w / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> -w / 3 } + fadeOut())
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            label = "MonthYearSwitch"
        ) { state ->
            // ✅ VIGTIGT: BoxScope så align virker korrekt
            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    CalendarZoom.MONTH -> {
                        Text(
                            text = yearText,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clickable { onZoomChange(CalendarZoom.YEAR) }
                                .padding(horizontal = 4.dp, vertical = 6.dp), // større touch
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )

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
                            text = yearText,
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
                                .padding(horizontal = 4.dp, vertical = 6.dp), // større touch
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }

                    CalendarZoom.WEEK -> {
                        Text(
                            text = monthText,
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ---- Chevron (ALWAYS collapse fully to WEEK) ----
        IconButton(
            onClick = { onZoomChange(CalendarZoom.WEEK) },
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