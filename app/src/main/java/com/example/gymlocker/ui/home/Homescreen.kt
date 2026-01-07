package com.example.gymlocker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.theme.GymLockerTheme
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, activeWorkoutViewModel: ActiveWorkoutViewModel) {
    val isWorkoutInProgress by activeWorkoutViewModel.isWorkoutInProgress.collectAsState()
    val elapsedTime by activeWorkoutViewModel.elapsedTime.collectAsState()
    val completedWorkouts by activeWorkoutViewModel
        .completedWorkouts()
        .collectAsState(initial = emptyList())

    // --- NEW: Query workouts in current week (Mon–Sun) from ExerciseLogDao (only "completed" workouts) ---
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val exerciseLogDao = remember { db.exerciseLogDao() }

    // Same date format as Workout.date in your DB: "yyyy-MM-dd HH:mm:ss.SSS"
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") }

    val today = LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val endOfWeek = startOfWeek.plusDays(6)

    val startInclusive = startOfWeek.atStartOfDay().format(formatter)
    val endInclusive = endOfWeek.atTime(23, 59, 59, 999_000_000).format(formatter)

    // IMPORTANT: requires this DAO method:
    // fun observeCompletedWorkoutCountInRange(startInclusive: String, endInclusive: String): Flow<Int>
    val workoutsThisWeek by exerciseLogDao
        .observeCompletedWorkoutCountInRange(
            startInclusive = startInclusive,
            endInclusive = endInclusive
        )
        .collectAsState(initial = 0)
    // ---------------------------------------------------------------------------------------------

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        },
        bottomBar = {
            Column {
                if (isWorkoutInProgress) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { navController.navigate("activeWorkout") }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Workout in Progress")
                            Text(activeWorkoutViewModel.formatTime(elapsedTime))
                        }
                    }
                }
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Filled.Home, contentDescription = "Home")
                        }
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                if (isWorkoutInProgress) {
                    navController.navigate("activeWorkout")
                } else {
                    navController.navigate("workout")
                }
            }) {
                Text(if (isWorkoutInProgress) "Resume Workout" else "Start Workout")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly consistency (AC: show 0 if none)
            WeeklyWorkoutsCard(workoutsThisWeek = workoutsThisWeek)

            Spacer(modifier = Modifier.height(16.dp))
            StatsCard()
            Spacer(modifier = Modifier.height(16.dp))
            CompletedWorkoutsCard(
                workouts = completedWorkouts,
                onViewHistoryClick = { navController.navigate("workoutHistory") }
            )
        }
    }
}

@Composable
fun WeeklyWorkoutsCard(workoutsThisWeek: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This week", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("$workoutsThisWeek workouts this week")
        }
    }
}

@Composable
fun StatsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Stats")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Graph will be here")
        }
    }
}

@Composable
fun CompletedWorkoutsCard(
    workouts: List<WorkoutSummary>,
    onViewHistoryClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Completed Workouts")
            Spacer(modifier = Modifier.height(8.dp))

            if (workouts.isEmpty()) {
                Text("No completed workouts yet.", textAlign = TextAlign.Center)
            } else {
                workouts.take(5).forEach { w ->
                    Text(
                        text = "• ${w.name} — ${formatWorkoutDatePretty(w.date)} • ${w.exerciseCount} exercises"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Workout History")
                }
            }
        }
    }
}

/**
 * Converts:
 * "2026-01-07 14:33:12.456" → "Jan 7, 2026"
 */
private fun formatWorkoutDatePretty(raw: String): String {
    return try {
        val datePart = raw.substringBefore(" ")
        val parsed = LocalDate.parse(datePart)
        parsed.format(
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        )
    } catch (e: Exception) {
        raw.substringBefore(" ")
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GymLockerTheme {
        HomeScreen(rememberNavController(), viewModel())
    }
}
