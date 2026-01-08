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
import com.example.gymlocker.data.dao.WorkoutSummary
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.theme.GymLockerTheme
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.temporal.ChronoUnit
import com.example.gymlocker.data.entity.template.WorkoutTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, activeWorkoutViewModel: ActiveWorkoutViewModel) {
    val isWorkoutInProgress by activeWorkoutViewModel.isWorkoutInProgress.collectAsState()
    val elapsedTime by activeWorkoutViewModel.elapsedTime.collectAsState()
    val completedWorkouts by activeWorkoutViewModel
        .completedWorkouts()
        .collectAsState(initial = emptyList())
    val lastWorkoutLabel by activeWorkoutViewModel
        .lastWorkoutLabel()
        .collectAsState(initial = "Finder seneste workout…")

    // --- Query workouts in current week (Mon–Sun) from ExerciseLogDao (only "completed" workouts) ---
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

    val workoutsThisWeek by exerciseLogDao
        .observeCompletedWorkoutCountInRange(
            startInclusive = startInclusive,
            endInclusive = endInclusive
        )
        .collectAsState(initial = 0)
    // ---------------------------------------------------------------------------------------------

    // Observe templates for default user (1L)
    val templatesFlow = activeWorkoutViewModel.observeTemplates(1L)
    val templates by templatesFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) },
        bottomBar = {
            Column {
                // (1) Active workout banner (hvis i gang)
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

                // (2) Thumb-friendly primary action button
                Button(
                    onClick = {
                        if (isWorkoutInProgress) {
                            navController.navigate("activeWorkout")
                        } else {
                            navController.navigate("workout")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isWorkoutInProgress) "Resume Workout" else "Start Workout")
                }

                // (3) Bottom nav bar
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
            Text(
                text = lastWorkoutLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            WeeklyWorkoutsCard(workoutsThisWeek = workoutsThisWeek)

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
            Button(onClick = {
                navController.navigate("createTemplate")
            }) {
                Text("Opret nyt template")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TemplatesCard(templates) { templateId ->
                navController.navigate("templateDetail/$templateId")
            }
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

/**
 * ✅ Pretty date:
 * Input: "yyyy-MM-dd HH:mm:ss.SSS"
 * Output: "Jan 7 2026"
 */
private fun prettyWorkoutDate(raw: String): String {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)
        LocalDateTime.parse(raw, input).format(output)
    } catch (e: Exception) {
        raw // fallback
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
                // Show latest 5
                workouts.take(5).forEach { w ->
                    val prettyDate = prettyWorkoutDate(w.date)
                    Text("• ${w.name} - $prettyDate - ${w.exerciseCount} exercises")
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

@Composable
fun TemplatesCard(templates: List<WorkoutTemplate>, onStartFromTemplate: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Templates")
            Spacer(modifier = Modifier.height(8.dp))
            if (templates.isEmpty()) {
                Text("No templates yet.")
            } else {
                templates.take(5).forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartFromTemplate(t.templateId) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t.name)
                        Text(t.date)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GymLockerTheme {
        HomeScreen(rememberNavController(), viewModel())
    }
}
