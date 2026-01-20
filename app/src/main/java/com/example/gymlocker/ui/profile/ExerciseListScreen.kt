package com.example.gymlocker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
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

data class ExerciseStatsData(
    val exercise: Exercises,
    val muscleGroupName: String,
    val personalRecord: String,
    val lastTrained: String,
    val totalSets: Int,
    val totalVolume: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    val activeProfileUserId by profileViewModel.activeProfileUserId.collectAsState()
    val activeProfile by profileViewModel.activeProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroupId by remember { mutableStateOf<Long?>(null) }
    var showMuscleGroupMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    val allExercises by db.exerciseDao().getAllExercises().collectAsState(initial = emptyList())
    val allMuscleGroups by db.muscleGroupDao().getAllMuscleGroups().collectAsState(initial = emptyList())

    var exerciseStatsMap by remember { mutableStateOf<Map<Long, ExerciseStatsData>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(activeProfileUserId, allExercises) {
        if (activeProfileUserId == null) {
            isLoading = false
            return@LaunchedEffect
        }

        scope.launch {
            isLoading = true
            val statsMap = mutableMapOf<Long, ExerciseStatsData>()

            allExercises.forEach { exercise ->
                val muscleGroupName =
                    db.muscleGroupDao().getNameById(exercise.muscleGroupId) ?: "Unknown"

                val prSet = db.performedSetDao().getPersonalRecordSetForExerciseExcludingWorkout(
                    exerciseId = exercise.exerciseId,
                    excludeWorkoutId = null
                )
                val prText = if (prSet != null && prSet.reps > 0) {
                    "${prSet.weight.toInt()} kg × ${prSet.reps}"
                } else if (prSet != null) {
                    "${prSet.weight.toInt()} kg"
                } else {
                    "No data"
                }

                val lastDate = db.performedSetDao().getLastTrainedDateForExerciseExcludingWorkout(
                    exerciseId = exercise.exerciseId,
                    excludeWorkoutId = null
                )
                val lastTrainedText = if (lastDate != null) formatDateRelative(lastDate) else "Never trained"

                val totalSets = db.performedSetDao().getTotalSetsForExercise(
                    userId = activeProfileUserId!!,
                    exerciseId = exercise.exerciseId
                )
                val totalVolume = db.performedSetDao().getTotalVolumeForExercise(
                    userId = activeProfileUserId!!,
                    exerciseId = exercise.exerciseId
                )

                if (totalSets > 0) {
                    statsMap[exercise.exerciseId] = ExerciseStatsData(
                        exercise = exercise,
                        muscleGroupName = muscleGroupName,
                        personalRecord = prText,
                        lastTrained = lastTrainedText,
                        totalSets = totalSets,
                        totalVolume = totalVolume
                    )
                }
            }

            exerciseStatsMap = statsMap
            isLoading = false
        }
    }

    val filteredExercises = exerciseStatsMap.values
        .filter { stats ->
            val matchesSearch = searchQuery.isBlank() ||
                    stats.exercise.name.contains(searchQuery, ignoreCase = true)
            val matchesMuscleGroup = selectedMuscleGroupId == null ||
                    stats.exercise.muscleGroupId == selectedMuscleGroupId
            matchesSearch && matchesMuscleGroup
        }
        .sortedByDescending { it.totalVolume }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search exercises...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        Text("Exercise Statistics")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
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
        if (activeProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "No profile selected",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Select a profile to view exercise statistics",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("profile") }) {
                        Text("Go to Profile")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .metalGloss(),
                border = cardBorder,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter:", style = MaterialTheme.typography.labelLarge)

                    FilterChip(
                        selected = selectedMuscleGroupId == null,
                        onClick = { selectedMuscleGroupId = null },
                        label = { Text("All") }
                    )

                    Box {
                        FilterChip(
                            selected = selectedMuscleGroupId != null,
                            onClick = { showMuscleGroupMenu = true },
                            label = {
                                val selectedName = allMuscleGroups
                                    .firstOrNull { it.muscleGroupId == selectedMuscleGroupId }
                                    ?.name
                                Text(selectedName ?: "Muscle Group")
                            }
                        )

                        DropdownMenu(
                            expanded = showMuscleGroupMenu,
                            onDismissRequest = { showMuscleGroupMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            allMuscleGroups.forEach { mg ->
                                DropdownMenuItem(
                                    text = { Text(mg.name, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedMuscleGroupId = mg.muscleGroupId
                                        showMuscleGroupMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                filteredExercises.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = if (exerciseStatsMap.isEmpty())
                                    "No exercises trained yet"
                                else
                                    "No exercises match your filters",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Start a workout to see exercise statistics here",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${filteredExercises.size} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(filteredExercises, key = { it.exercise.exerciseId }) { stats ->
                            ExerciseStatsCard(
                                stats = stats,
                                cardBorder = cardBorder,
                                onClick = {
                                    navController.navigate("exerciseDetail/${stats.exercise.exerciseId}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseStatsCard(
    stats: ExerciseStatsData,
    cardBorder: BorderStroke,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .metalGloss()
            .clickable(onClick = onClick),
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
                        text = stats.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stats.muscleGroupName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(label = "Personal Best", value = stats.personalRecord)
                StatColumn(label = "Total Sets", value = stats.totalSets.toString())
                StatColumn(label = "Total Volume", value = formatVolume(stats.totalVolume))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Last trained: ${stats.lastTrained}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun formatDateRelative(dateString: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val date = LocalDateTime.parse(dateString, formatter)
        val now = LocalDateTime.now()

        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(date.toLocalDate(), now.toLocalDate())

        when {
            daysBetween == 0L -> "Today"
            daysBetween == 1L -> "Yesterday"
            daysBetween < 7 -> "$daysBetween days ago"
            daysBetween < 30 -> "${daysBetween / 7} weeks ago"
            daysBetween < 365 -> "${daysBetween / 30} months ago"
            else -> "${daysBetween / 365} years ago"
        }
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
