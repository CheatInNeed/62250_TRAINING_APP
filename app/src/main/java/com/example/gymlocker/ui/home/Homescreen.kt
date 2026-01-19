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
import com.example.gymlocker.ui.components.WeeklyBarChart
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

        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WeeklyWorkoutsCircles(workoutDates = workoutDates)
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
                sections.forEach { (date, itemsForDate) ->
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
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {

                                // Lille primary dot – samme som på cards
                                Text(
                                    text = homeSectionTitleForDate(date),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
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
        }
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

enum class WeeklyGraphMode { HOURS, VOLUME }

@Composable
fun StatsCard(
    weeklyHours: List<com.example.gymlocker.viewmodel.WeekHoursUi>,
    weeklyVolume: List<com.example.gymlocker.viewmodel.WeekVolumeUi>,
    distribution: List<com.example.gymlocker.data.dao.MuscleGroupDistributionRow>,
    statsRange: StatsRange,
    onRangeChange: (StatsRange) -> Unit
) {
    var mode by remember { mutableStateOf(WeeklyGraphMode.HOURS) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Stats",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedToggle(
                    leftText = "Week",
                    rightText = "Month",
                    isLeftSelected = statsRange == StatsRange.WEEK,
                    onLeftClick = { onRangeChange(StatsRange.WEEK) },
                    onRightClick = { onRangeChange(StatsRange.MONTH) },
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary
                )

                SegmentedToggle(
                    leftText = "Hours",
                    rightText = "Volume",
                    isLeftSelected = mode == WeeklyGraphMode.HOURS,
                    onLeftClick = { mode = WeeklyGraphMode.HOURS },
                    onRightClick = { mode = WeeklyGraphMode.VOLUME },
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (mode == WeeklyGraphMode.HOURS)
                    "Hours trained per week (last 3 months)"
                else
                    "Volume per week (last 3 months)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (mode == WeeklyGraphMode.HOURS) {
                WeeklyBarChart(
                    data = weeklyHours,
                    weekStartOf = { it.weekStart },
                    valueOf = { it.hours },
                    modifier = Modifier.fillMaxWidth(),
                    legendPrefix = "Week:"
                )
            } else {
                WeeklyBarChart(
                    data = weeklyVolume,
                    weekStartOf = { it.weekStart },
                    valueOf = { it.volume },
                    modifier = Modifier.fillMaxWidth(),
                    legendPrefix = "Week:"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (statsRange == StatsRange.WEEK)
                    "Training balance (this week)"
                else
                    "Training balance (this month)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            MuscleGroupDistributionChart(
                rows = distribution,
                modifier = Modifier.fillMaxWidth()
            )
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
    workoutDates: Set<LocalDate>,
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
            val trained = workoutDates.contains(date)

            WeekDayCircleColumn(
                dayLabel = dayLetter(date.dayOfWeek),
                numberLabel = date.dayOfMonth.toString(),
                trained = trained
            )
        }
    }
}


@Composable
private fun WeekDayCircleColumn(
    dayLabel: String,
    numberLabel: String,
    trained: Boolean
) {
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val fill = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall,
            //fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp) // lidt større end før
                .clip(CircleShape)
                .background(if (trained) fill.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)
                .border(1.dp, outline, CircleShape) // matcher cards: 1dp + primary alpha 0.28
        ) {
            Text(
                text = numberLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        }
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