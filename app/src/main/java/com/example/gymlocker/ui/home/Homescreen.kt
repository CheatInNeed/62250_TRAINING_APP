package com.example.gymlocker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.dao.WorkoutSummary
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.AppTheme
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.history.HistoryViewMode
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
        .collectAsState(initial = "Finding latest workout...")

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

    // Force 16.dp rounded corners on ALL cards in this file
    val cardShape = remember { RoundedCornerShape(16.dp) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Peter Standard: top bar uses metalGloss(TopBarShape)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .metalGloss(TopBarShape)
            ) {
                TopAppBar(
                    title = { Text("Workout Feed") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        // Match other topbars: use surface (not transparent / background)
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.metalGloss(BotBarShape), // ✅ required: metalGloss(BotBarShape)
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
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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

        // (kept as-is; not shown in this snippet)
        val weeklyVolume by statViewModel.weeklyVolumeLast3Months(userId).collectAsState(initial = emptyList())
        val weeklyHours by statViewModel.weeklyHoursLast3Months(userId).collectAsState(initial = emptyList())
        val statsRange by statViewModel.statsRange.collectAsState()
        val distribution by statViewModel.muscleGroupDistribution(userId).collectAsState(initial = emptyList())

        var historyViewMode by remember { mutableStateOf(HistoryViewMode.LIST) }

        val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

        val sections = remember(completedWorkouts) {
            completedWorkouts
                .map { ws -> ws to ws.homeSafeLocalDateTime(formatter) }
                .sortedByDescending { (_, dt) -> dt ?: LocalDateTime.MIN }
                .groupBy { (_, dt) -> dt?.toLocalDate() }
                .toSortedMap(compareByDescending { it })
        }

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)

        val workoutsPerDay = remember(sections) {
            sections
                .filterKeys { it != null }
                .mapKeys { it.key!! }
                .mapValues { (_, list) -> list.size }
        }

        val listState = rememberLazyListState()

        var calendarExpanded by rememberSaveable { mutableStateOf(false) }
        var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
        var currentMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }

        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .map { (idx, off) -> idx > 0 || off > 0 }
                .distinctUntilChanged()
                .collect { isScrollingDown ->
                    if (isScrollingDown) calendarExpanded = false
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
                    expanded = calendarExpanded,
                    onExpandedChange = { calendarExpanded = it },
                    onMonthChange = { currentMonth = it },
                    onDateSelected = { date ->
                        selectedDate = date
                        currentMonth = YearMonth.from(date)
                    },
                    cardShape = cardShape
                )
            }

            if (sections.isEmpty()) {
                item {
                    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .metalGloss(cardShape),
                        shape = cardShape,
                        border = cardBorder,
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
                if (calendarExpanded) {
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
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
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
                            val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .metalGloss(cardShape),
                                shape = cardShape,
                                border = cardBorder,
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
                                onClick = { navController.navigate("workoutDetail/${ws.workoutId}") },
                                cardShape = cardShape
                            )
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
                                onClick = { navController.navigate("workoutDetail/${ws.workoutId}") },
                                cardShape = cardShape
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
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
    onClick: () -> Unit,
    cardShape: RoundedCornerShape
) {
    val timeText = remember(dateTime) { dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty() }

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
            .metalGloss(cardShape),
        shape = cardShape,
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column {
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
                        val parsed = homeParseSetSummaryLine(line)
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

private fun homeParseSetSummaryLine(line: String): Pair<Int, String>? {
    val regex = Regex("""^\s*(\d+)\s*x\s*(.+?)\s*$""")
    val match = regex.find(line) ?: return null
    val count = match.groupValues[1].toIntOrNull() ?: return null
    val name = match.groupValues[2]
    return count to name
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

@Composable
private fun WorkoutDotsRow(count: Int) {
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
private fun ExpandableHomeCalendarHeader(
    workoutsPerDay: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    cardShape: RoundedCornerShape
) {
    val today = remember { LocalDate.now() }
    val weekDays = remember(today) { (6L downTo 0L).map { today.minusDays(it) } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(expanded) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (!expanded && dragAmount > 18f) onExpandedChange(true)
                        if (expanded && dragAmount < -18f) onExpandedChange(false)
                    }
                )
            }
    ) {
        // Month title row (card-like): should be metalGloss + outline + 16.dp shape
        val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .metalGloss(cardShape),
            shape = cardShape,
            border = cardBorder,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val title =
                    "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        AnimatedVisibility(
            visible = !expanded,
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
                        selected = (date == selectedDate),
                        onClick = {
                            onDateSelected(date)
                            onExpandedChange(true)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
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
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev month")
                    }
                    Text(
                        text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }

                HomeCalendarGridCounts(
                    currentMonth = currentMonth,
                    workoutsPerDay = workoutsPerDay,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Collapse",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 6.dp)
                        .clickable { onExpandedChange(false) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onBackground
                            )

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
