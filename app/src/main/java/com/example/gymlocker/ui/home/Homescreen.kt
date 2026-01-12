package com.example.gymlocker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
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
import com.example.gymlocker.ui.components.*
import com.example.gymlocker.ui.components.MuscleGroupDistributionChart
import com.example.gymlocker.ui.components.WeeklyBarChart
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import com.example.gymlocker.data.auth.SessionManager



import kotlinx.coroutines.flow.flowOf

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

    val weeklyVolume by activeWorkoutViewModel
        .weeklyVolumeLast3Months(1L)
        .collectAsState(initial = emptyList())


    // --- Query workouts in current week (Mon–Sun) from ExerciseLogDao (only "completed" workouts) ---
    val context = LocalContext.current

    // ✅ Active profile
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

    // ✅ FIX: weekly count per active profile
    val workoutsThisWeek by (if (activeProfileUserId == null) {
        flowOf(0)
    } else {
        exerciseLogDao.observeCompletedWorkoutCountInRangeForUser(
            userId = activeProfileUserId!!,
            startInclusive = startInclusive,
            endInclusive = endInclusive
        )
    }).collectAsState(initial = 0)
    // ---------------------------------------------

    val weeklyHours by activeWorkoutViewModel
        .weeklyHoursLast3Months(1L)
        .collectAsState(initial = emptyList())

    val statsRange by activeWorkoutViewModel.statsRange.collectAsState()
    val distribution by activeWorkoutViewModel.muscleGroupDistribution(1L).collectAsState(initial = emptyList())



    // ---------------------------------------------------------------------------------------------

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

        // ✅ Bottom bar: ONE start/resume button + ONE active workout banner + the nav bar
        bottomBar = {
            Column {
                // Thumb-friendly primary action button (ONLY PLACE IT EXISTS)
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

                // Reusable banner + nav bar
                ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->

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

            // ✅ Removed the duplicate Start/Resume button that was in the list

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

            item { StatsCard(
                weeklyHours = weeklyHours,
                weeklyVolume = weeklyVolume,
                distribution = distribution,
                statsRange = statsRange,
                onRangeChange = { activeWorkoutViewModel.setStatsRange(it) }
            ) }


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
private fun SegmentedToggle(
    leftText: String,
    rightText: String,
    isLeftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColors = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    val unselectedColors = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onLeftClick,
            shape = RoundedCornerShape(999.dp),
            colors = if (isLeftSelected) selectedColors else unselectedColors,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) { Text(leftText) }

        Button(
            onClick = onRightClick,
            shape = RoundedCornerShape(999.dp),
            colors = if (!isLeftSelected) selectedColors else unselectedColors,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) { Text(rightText) }
    }
}

enum class WeeklyGraphMode { HOURS, VOLUME }

@Composable
fun StatsCard(
    weeklyHours: List<com.example.gymlocker.viewmodel.WeekHoursUi>,
    weeklyVolume: List<com.example.gymlocker.viewmodel.WeekVolumeUi>,
    distribution: List<com.example.gymlocker.data.dao.MuscleGroupDistributionRow>,
    statsRange: com.example.gymlocker.viewmodel.StatsRange,
    onRangeChange: (com.example.gymlocker.viewmodel.StatsRange) -> Unit
) {
    var mode by remember { mutableStateOf(WeeklyGraphMode.HOURS) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Stats", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            // ===== Toggles row (Range + Mode) =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Week / Month (controls distribution range)
                SegmentedToggle(
                    leftText = "Week",
                    rightText = "Month",
                    isLeftSelected = statsRange == com.example.gymlocker.viewmodel.StatsRange.WEEK,
                    onLeftClick = { onRangeChange(com.example.gymlocker.viewmodel.StatsRange.WEEK) },
                    onRightClick = { onRangeChange(com.example.gymlocker.viewmodel.StatsRange.MONTH) }
                )

                // Hours / Volume (controls weekly chart mode)
                SegmentedToggle(
                    leftText = "Hours",
                    rightText = "Volume",
                    isLeftSelected = mode == WeeklyGraphMode.HOURS,
                    onLeftClick = { mode = WeeklyGraphMode.HOURS },
                    onRightClick = { mode = WeeklyGraphMode.VOLUME }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ===== Weekly chart =====
            Text(
                text = if (mode == WeeklyGraphMode.HOURS)
                    "Hours trained per week (last 3 months)"
                else
                    "Volume per week (last 3 months)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
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

            // ===== Distribution chart =====
            Text(
                text = if (statsRange == com.example.gymlocker.viewmodel.StatsRange.WEEK)
                    "Training balance (this week)"
                else
                    "Training balance (this month)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
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
