package com.example.gymlocker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.PerformedSet
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WorkoutSessionData(
    val workoutId: Long,
    val workoutName: String,
    val workoutDate: String,
    val sets: List<PerformedSet>,
    val totalVolume: Double,
    val maxWeight: Float,
    val totalReps: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val activeProfileUserId by profileViewModel.activeProfileUserId.collectAsState()

    var exerciseName by remember { mutableStateOf("Loading...") }
    var muscleGroupName by remember { mutableStateOf("") }
    var personalRecord by remember { mutableStateOf("No data") }
    var totalSets by remember { mutableStateOf(0) }
    var totalVolume by remember { mutableStateOf(0.0) }
    var workoutSessions by remember { mutableStateOf<List<WorkoutSessionData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    LaunchedEffect(exerciseId, activeProfileUserId) {
        if (activeProfileUserId == null) return@LaunchedEffect

        scope.launch {
            isLoading = true

            // Get exercise details
            val exercise = db.exerciseDao().getById(exerciseId)
            exerciseName = exercise?.name ?: "Unknown Exercise"
            muscleGroupName = db.muscleGroupDao()
                .getNameById(exercise?.muscleGroupId ?: 0) ?: ""

            // Get PR
            val prSet = db.performedSetDao().getPersonalRecordSetForExerciseExcludingWorkout(
                exerciseId = exerciseId,
                excludeWorkoutId = null
            )
            personalRecord = if (prSet != null && prSet.reps > 0) {
                "${prSet.weight.toInt()} kg × ${prSet.reps} reps"
            } else if (prSet != null) {
                "${prSet.weight.toInt()} kg"
            } else {
                "No data"
            }

            // Get totals
            totalSets = db.performedSetDao().getTotalSetsForExercise(
                userId = activeProfileUserId!!,
                exerciseId = exerciseId
            )
            totalVolume = db.performedSetDao().getTotalVolumeForExercise(
                userId = activeProfileUserId!!,
                exerciseId = exerciseId
            )

            // Get workout history for this exercise
            val workoutIds = db.performedSetDao().getWorkoutIdsForExercise(
                userId = activeProfileUserId!!,
                exerciseId = exerciseId
            )

            val sessions = workoutIds.mapNotNull { workoutId ->
                val workout = db.workoutDao().getWorkoutById(workoutId) ?: return@mapNotNull null
                val sets = db.performedSetDao().getPerformedSetsForExerciseInWorkout(
                    workoutId = workoutId,
                    exerciseId = exerciseId
                ).filter { it.isCompleted }

                if (sets.isEmpty()) return@mapNotNull null

                val volume = sets.sumOf { (it.weight * it.reps).toDouble() }
                val maxWeight = sets.maxOfOrNull { it.weight } ?: 0f
                val totalReps = sets.sumOf { it.reps }

                WorkoutSessionData(
                    workoutId = workoutId,
                    workoutName = workout.name,
                    workoutDate = workout.date,
                    sets = sets,
                    totalVolume = volume,
                    maxWeight = maxWeight,
                    totalReps = totalReps
                )
            }

            workoutSessions = sessions
            isLoading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = { Text(exerciseName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metalGloss(),
                    border = cardBorder,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            exerciseName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = muscleGroupName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Stats Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .metalGloss(),
                    border = cardBorder,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overall Statistics", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(label = "Personal Best", value = personalRecord)
                            StatColumn(label = "Total Sets", value = totalSets.toString())
                            StatColumn(
                                label = "Total Volume",
                                value = formatVolume(totalVolume)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "${workoutSessions.size} workouts performed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Workout History
            item {
                Text(
                    "Workout History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (workoutSessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .metalGloss(),
                        border = cardBorder,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No workout history for this exercise",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(workoutSessions, key = { it.workoutId }) { session ->
                    WorkoutSessionCard(
                        session = session,
                        cardBorder = cardBorder
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutSessionCard(
    session: WorkoutSessionData,
    cardBorder: BorderStroke
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .metalGloss(),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.workoutName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDate(session.workoutDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${session.sets.size} sets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Header row for sets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET",
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "KG",
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "REPS",
                    modifier = Modifier.weight(1.0f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(4.dp))

            // Display sets
            session.sets.forEachIndexed { index, set ->
                if (set.isCompleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set ${index + 1}",
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = set.weight.toInt().toString(),
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = set.reps.toString(),
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(label = "Volume", value = formatVolume(session.totalVolume))
                MiniStat(label = "Max Weight", value = "${session.maxWeight.toInt()} kg")
                MiniStat(label = "Total Reps", value = session.totalReps.toString())
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        LocalDateTime.parse(dateString, input).format(output)
    } catch (e: Exception) {
        dateString
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000 -> String.format(Locale.US, "%.1fM", volume / 1_000_000)
        volume >= 1_000 -> String.format(Locale.US, "%.1fK", volume / 1_000)
        else -> String.format(Locale.US, "%.0f", volume)
    }
}
