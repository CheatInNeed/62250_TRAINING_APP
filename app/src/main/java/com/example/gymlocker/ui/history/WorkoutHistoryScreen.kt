package com.example.gymlocker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
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
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class HistoryViewMode {
    LIST, CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val workouts by activeWorkoutViewModel
        .completedWorkouts()
        .collectAsState(initial = emptyList())

    var viewMode by remember { mutableStateOf(HistoryViewMode.LIST) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewMode = if (viewMode == HistoryViewMode.LIST) HistoryViewMode.CALENDAR else HistoryViewMode.LIST
                    }) {
                        Icon(
                            imageVector = if (viewMode == HistoryViewMode.LIST) Icons.Default.DateRange else Icons.AutoMirrored.Filled.List,
                            contentDescription = if (viewMode == HistoryViewMode.LIST) "Switch to Calendar" else "Switch to List"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (workouts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No completed workouts yet.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                if (viewMode == HistoryViewMode.LIST) {
                    WorkoutList(workouts)
                } else {
                    WorkoutCalendar(workouts)
                }
            }
        }
    }
}

@Composable
fun WorkoutList(workouts: List<WorkoutSummary>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(workouts) { workout ->
            ListItem(
                headlineContent = { Text(workout.date) },
                supportingContent = { Text("${workout.exerciseCount} exercises") }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun WorkoutCalendar(workouts: List<WorkoutSummary>) {
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

    Column(modifier = Modifier.fillMaxSize()) {
        CalendarHeader(
            currentMonth = currentMonth,
            onMonthChange = { currentMonth = it }
        )
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (dayWorkouts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No workouts on this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(dayWorkouts) { workout ->
                        ListItem(
                            headlineContent = { 
                                val time = try {
                                    LocalDateTime.parse(workout.date, formatter).format(DateTimeFormatter.ofPattern("HH:mm"))
                                } catch (e: Exception) {
                                    "Workout"
                                }
                                Text("Workout at $time") 
                            },
                            supportingContent = { Text("${workout.exerciseCount} exercises") }
                        )
                        HorizontalDivider()
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a day to see workouts", color = MaterialTheme.colorScheme.outline)
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
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
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
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // 0=Sun, 1=Mon...
    val daysInMonth = currentMonth.lengthOfMonth()
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
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
                                    hasWorkout -> MaterialTheme.colorScheme.primaryContainer
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
                                    hasWorkout -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
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
