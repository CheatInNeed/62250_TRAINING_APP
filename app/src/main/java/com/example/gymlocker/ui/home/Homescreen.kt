package com.example.gymlocker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.dao.WorkoutSummary
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.template.WorkoutTemplate
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.theme.GymLockerTheme
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Row


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val isWorkoutInProgress by activeWorkoutViewModel.isWorkoutInProgress.collectAsState()

    val completedWorkouts by activeWorkoutViewModel
        .completedWorkouts()
        .collectAsState(initial = emptyList())

    val lastWorkoutLabel by activeWorkoutViewModel
        .lastWorkoutLabel()
        .collectAsState(initial = "Finder seneste workout…")

    val context = LocalContext.current

    // ✅ Active profile (Phase 2 will add UI to create/select this)
    val session = remember { SessionManager(context.applicationContext) }
    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)

    // --- Query workouts in current week (Mon–Sun) ---
    val db = remember { AppDatabase.getDatabase(context) }
    val exerciseLogDao = remember { db.exerciseLogDao() }

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") }
    val today = LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val endOfWeek = startOfWeek.plusDays(6)

    val startInclusive = startOfWeek.atStartOfDay().format(formatter)
    val endInclusive = endOfWeek.atTime(23, 59, 59, 999_000_000).format(formatter)

    val workoutsThisWeek by exerciseLogDao
        .observeCompletedWorkoutCountInRange(startInclusive = startInclusive, endInclusive = endInclusive)
        .collectAsState(initial = 0)
    // ---------------------------------------------

    // ✅ Templates: only observe when we have an active profile
    val templatesFlow = remember(activeProfileUserId) {
        if (activeProfileUserId == null) null
        else activeWorkoutViewModel.observeTemplates(activeProfileUserId!!)
    }
    val templates by (templatesFlow?.collectAsState(initial = emptyList()) ?: remember {
        androidx.compose.runtime.mutableStateOf(emptyList<WorkoutTemplate>())
    })

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) },
        bottomBar = {
            Column {
                Button(
                    onClick = {
                        if (isWorkoutInProgress) navController.navigate("activeWorkout")
                        else navController.navigate("workout")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = activeProfileUserId != null
                ) {
                    Text(if (isWorkoutInProgress) "Resume Workout" else "Start Workout")
                }

                ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->

        // ✅ Phase-gate: no active profile selected yet
        // ✅ Phase-gate: no active profile selected yet
        if (activeProfileUserId == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Your profile stores your name, height, weight, and workout summary.\nYou can create one from the Profile page.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { navController.navigate("profile") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Profile")
                    }
                }
            }
            return@Scaffold
        }


        // ✅ Normal home content
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = lastWorkoutLabel,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item { WeeklyWorkoutsCard(workoutsThisWeek = workoutsThisWeek) }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Button(onClick = { navController.navigate("createTemplate") }) {
                    Text("Opret nyt template")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                TemplatesCard(templates) { templateId ->
                    navController.navigate("templateDetail/$templateId")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { StatsCard() }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                CompletedWorkoutsCard(
                    workouts = completedWorkouts,
                    onViewHistoryClick = { navController.navigate("workoutHistory") }
                )
            }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Completed Workouts")
            Spacer(modifier = Modifier.height(8.dp))

            if (workouts.isEmpty()) {
                Text("No completed workouts yet.", textAlign = TextAlign.Center)
            } else {
                workouts.take(5).forEach { w ->
                    val prettyDate = prettyWorkoutDate(w.date)
                    Text("• ${w.name} - $prettyDate - ${w.exerciseCount} exercises")
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Workout History") }
            }
        }
    }
}

@Composable
fun TemplatesCard(
    templates: List<WorkoutTemplate>,
    onStartFromTemplate: (Long) -> Unit
) {
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
