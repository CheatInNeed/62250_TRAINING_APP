package com.example.gymlocker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.dao.WorkoutSummary
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.WorkoutHistoryViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.BorderStroke


enum class HistoryViewMode { LIST, CALENDAR }

private fun prettyWorkoutDate(raw: String): String {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)
        LocalDateTime.parse(raw, input).format(output)
    } catch (e: Exception) {
        raw
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    viewModel: WorkoutHistoryViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val workouts by viewModel.completedWorkouts().collectAsState(initial = emptyList())
    var viewMode by remember { mutableStateOf(HistoryViewMode.LIST) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewMode =
                                if (viewMode == HistoryViewMode.LIST) HistoryViewMode.CALENDAR
                                else HistoryViewMode.LIST
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == HistoryViewMode.LIST) Icons.Default.DateRange else Icons.AutoMirrored.Filled.List,
                            contentDescription = if (viewMode == HistoryViewMode.LIST) "Switch to Calendar" else "Switch to List",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (workouts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No completed workouts yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                if (viewMode == HistoryViewMode.LIST) {
                    WorkoutList(
                        workouts = workouts,
                        onWorkoutClick = { workoutId -> navController.navigate("workoutDetail/$workoutId") }
                    )
                } else {
                    WorkoutCalendar(
                        workouts = workouts,
                        onWorkoutClick = { workoutId -> navController.navigate("workoutDetail/$workoutId") }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutList(
    workouts: List<WorkoutSummary>,
    onWorkoutClick: (Long) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") }

    // Parse once, sort newest first, and group into human-friendly sections
    val sections = remember(workouts) {
        workouts
            .map { ws -> ws to ws.safeLocalDateTime(formatter) }
            .sortedByDescending { (_, dt) -> dt ?: LocalDateTime.MIN }
            .groupBy { (_, dt) -> dt?.toLocalDate() }
            .toSortedMap(compareByDescending { it }) // newest date section first
    }

    if (workouts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No completed workouts yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sections.forEach { (date, itemsForDate) ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sectionTitleForDate(date),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            items(itemsForDate, key = { (ws, _) -> ws.workoutId }) { (ws, dt) ->
                WorkoutHistoryCard(
                    workout = ws,
                    dateTime = dt,
                    onClick = { onWorkoutClick(ws.workoutId) }
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) } // breathing room above bottom bar
    }
}

@Composable
private fun WorkoutHistoryCard(
    workout: WorkoutSummary,
    dateTime: LocalDateTime?,
    onClick: () -> Unit
) {
    val timeText = remember(dateTime) {
        dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
    }

    // Parse: "5x Bench Press, 3x Tricep Pushdown"
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
            MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column {

            // --- Accent strip (tertiary) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))
            )

            Column(modifier = Modifier.padding(16.dp)) {

                // Header: name (left) + time (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // --- lille "alive" dot (primary) ---
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.90f))
                    )

                    Spacer(Modifier.width(10.dp))

                    // Workout name
                    Text(
                        text = workout.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    // Time
                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Chevron
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
                        // line expected like "5x Bench Press"
                        val parsed = parseSetSummaryLine(line) // (count, name)
                        val count = parsed?.first
                        val exName = parsed?.second

                        val text = if (count != null && exName != null) {
                            val unit = if (count == 1) "set" else "sets"
                            "$count $unit of $exName"
                        } else {
                            // fallback hvis format ikke matcher
                            line
                        }

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

private fun parseSetSummaryLine(line: String): Pair<Int, String>? {
    // Accepts "5x Bench Press" OR "5 x Bench Press"
    val regex = Regex("""^\s*(\d+)\s*x\s*(.+?)\s*$""")
    val match = regex.find(line) ?: return null
    val count = match.groupValues[1].toIntOrNull() ?: return null
    val name = match.groupValues[2]
    return count to name
}

private fun WorkoutSummary.safeLocalDateTime(formatter: DateTimeFormatter): LocalDateTime? {
    return try {
        LocalDateTime.parse(this.date, formatter)
    } catch (_: Exception) {
        null
    }
}

private fun sectionTitleForDate(date: LocalDate?): String {
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

/* ---- Calendar ---- */

@Composable
fun WorkoutCalendar(
    workouts: List<WorkoutSummary>,
    onWorkoutClick: (Long) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") }

    val workoutsByDate = remember(workouts) {
        workouts.groupBy {
            try {
                LocalDateTime.parse(it.date, formatter).toLocalDate()
            } catch (e: Exception) {
                null
            }
        }.filterKeys { it != null }.mapKeys { it.key!! }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CalendarHeader(currentMonth = currentMonth, onMonthChange = { currentMonth = it })

        CalendarGrid(
            currentMonth = currentMonth,
            workoutsByDate = workoutsByDate,
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        val displayDate = selectedDate
        if (displayDate != null) {
            val dayWorkouts = workoutsByDate[displayDate] ?: emptyList()

            Text(
                text = displayDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (dayWorkouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No workouts on this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    items(dayWorkouts) { workout ->
                        val time = try {
                            LocalDateTime.parse(workout.date, formatter)
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Exception) {
                            null
                        }

                        val line = if (time != null) {
                            "${workout.name} - $time - ${workout.exerciseCount} exercises"
                        } else {
                            "${workout.name} - ${workout.exerciseCount} exercises"
                        }

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = line,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            modifier = Modifier
                                .clickable { onWorkoutClick(workout.workoutId) }
                                .background(MaterialTheme.colorScheme.surface),
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                headlineColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        HorizontalDivider()
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Select a day to see workouts",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    workoutsByDate: Map<LocalDate, List<WorkoutSummary>>,
    selectedDate: LocalDate?,
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
                    // Use onBackground for header labels (avoid random secondary)
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
                    val hasWorkout = workoutsByDate.containsKey(date)
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()

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
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    hasWorkout -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onBackground
                                },
                                fontWeight = if (isToday || hasWorkout) FontWeight.Bold else FontWeight.Normal
                            )
                            if (hasWorkout && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
