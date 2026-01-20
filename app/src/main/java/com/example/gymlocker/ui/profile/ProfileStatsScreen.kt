package com.example.gymlocker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.components.MuscleGroupDistributionChart
import com.example.gymlocker.ui.components.PeriodBarChart
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import com.example.gymlocker.viewmodel.StatViewModel
import com.example.gymlocker.viewmodel.StatsRange
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileStatsScreen(
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val statViewModel: StatViewModel = viewModel()

    val activeProfile by profileViewModel.activeProfile.collectAsState()
    val activeProfileUserId by profileViewModel.activeProfileUserId.collectAsState()
    val workoutSummary by profileViewModel.workoutSummary.collectAsState()

    // Date calculations for this week
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .toLocalDate().atStartOfDay()
    val startInclusive = monday.format(formatter)
    val endInclusive = now.plusDays(1)
        .withHour(0).withMinute(0).withSecond(0).withNano(0)
        .format(formatter)

    // Database access for additional stats
    val db = remember { AppDatabase.getDatabase(context) }
    val exerciseLogDao = remember { db.exerciseLogDao() }
    val workoutDao = remember { db.workoutDao() } // kept if used elsewhere later

    // Stats data
    val userId = activeProfileUserId
    val workoutsThisWeek by if (userId != null) {
        exerciseLogDao.observeCompletedWorkoutCountInRangeForUser(
            userId = userId,
            startInclusive = startInclusive,
            endInclusive = endInclusive
        ).collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    val weeklyVolume by if (userId != null) {
        statViewModel.weeklyVolumeLast3Months(userId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val weeklyHours by if (userId != null) {
        statViewModel.weeklyHoursLast3Months(userId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val statsRange by statViewModel.statsRange.collectAsState()
    val distribution by if (userId != null) {
        statViewModel.muscleGroupDistribution(userId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    // Calculate totals
    val totalHours = weeklyHours.sumOf { it.hours.toDouble() }
    val totalVolume = weeklyVolume.sumOf { it.volume.toDouble() }
    val avgHoursPerWeek = if (weeklyHours.isNotEmpty()) totalHours / weeklyHours.size else 0.0
    val avgVolumePerWeek = if (weeklyVolume.isNotEmpty()) totalVolume / weeklyVolume.size else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = {
                    Text("${activeProfile?.name ?: "Profile"} Statistics")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        if (activeProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No profile selected",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Select a profile to view statistics",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate("profile") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Go to Profile")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metalGloss(),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Overview",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                value = workoutSummary.totalWorkouts.toString(),
                                label = "Total Workouts"
                            )
                            StatItem(
                                value = workoutsThisWeek.toString(),
                                label = "This Week"
                            )
                            StatItem(
                                value = String.format(Locale.US, "%.1f", totalHours),
                                label = "Total Hours"
                            )
                        }

                        if (workoutSummary.mostRecentName != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Last workout: ${workoutSummary.mostRecentName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            workoutSummary.mostRecentDate?.let { date ->
                                Text(
                                    text = prettyDate(date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Weekly Averages Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metalGloss(),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "3-Month Averages",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                value = String.format(Locale.US, "%.1f", avgHoursPerWeek),
                                label = "Hours/Week"
                            )
                            StatItem(
                                value = formatVolume(avgVolumePerWeek),
                                label = "Volume/Week"
                            )
                        }
                    }
                }
            }

            // Weekly Progress Chart
            item {
                WeeklyProgressCard(
                    weeklyHours = weeklyHours,
                    weeklyVolume = weeklyVolume
                )
            }

            // Muscle Group Distribution
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metalGloss(),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Training Balance",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = statsRange == StatsRange.WEEK,
                                    onClick = { statViewModel.setStatsRange(StatsRange.WEEK) },
                                    label = { Text("Week") }
                                )
                                FilterChip(
                                    selected = statsRange == StatsRange.MONTH,
                                    onClick = { statViewModel.setStatsRange(StatsRange.MONTH) },
                                    label = { Text("Month") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (statsRange == StatsRange.WEEK)
                                "Sets per muscle group (this week)"
                            else
                                "Sets per muscle group (this month)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MuscleGroupDistributionChart(
                            rows = distribution,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Profile Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metalGloss(),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Profile Info",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val heightText = if (activeProfile!!.height == 0) "Not set" else "${activeProfile!!.height} cm"
                        val weightText = if (activeProfile!!.weight == 0) "Not set" else "${activeProfile!!.weight} kg"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                value = heightText,
                                label = "Height"
                            )
                            StatItem(
                                value = weightText,
                                label = "Weight"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { navController.navigate("editProfile") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Edit Profile")
                        }
                    }
                }
            }

            // Exercise List Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metalGloss(),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Exercise Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "View detailed statistics for each exercise including personal records, progression, and total volume",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { navController.navigate("exerciseList") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Exercise List")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private enum class ChartMode { HOURS, VOLUME }

@Composable
private fun WeeklyProgressCard(
    weeklyHours: List<com.example.gymlocker.viewmodel.WeekHoursUi>,
    weeklyVolume: List<com.example.gymlocker.viewmodel.WeekVolumeUi>
) {
    var mode by remember { mutableStateOf(ChartMode.HOURS) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .metalGloss(),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Weekly Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == ChartMode.HOURS,
                        onClick = { mode = ChartMode.HOURS },
                        label = { Text("Hours") }
                    )
                    FilterChip(
                        selected = mode == ChartMode.VOLUME,
                        onClick = { mode = ChartMode.VOLUME },
                        label = { Text("Volume") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (mode == ChartMode.HOURS)
                    "Hours trained per week (last 3 months)"
                else
                    "Volume per week (last 3 months)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(12.dp))

            val weekLabels = weeklyHours.map {
                it.weekStart.get(WeekFields.ISO.weekOfWeekBasedYear()).toString()
            }

            if (mode == ChartMode.HOURS) {
                PeriodBarChart(
                    values = weeklyHours.map { it.hours },
                    labels = weekLabels,
                    xCaption = "Week",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PeriodBarChart(
                    values = weeklyVolume.map { it.volume },
                    labels = weekLabels,
                    xCaption = "Week",
                    yTickStep = 500f, // 500 kg per tick
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun prettyDate(raw: String): String {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        LocalDateTime.parse(raw, input).format(output)
    } catch (e: Exception) {
        raw
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000 -> String.format(Locale.US, "%.1fM", volume / 1_000_000)
        volume >= 1_000 -> String.format(Locale.US, "%.1fK", volume / 1_000)
        else -> String.format(Locale.US, "%.0f", volume)
    }
}
