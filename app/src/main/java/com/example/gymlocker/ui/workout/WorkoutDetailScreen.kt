package com.example.gymlocker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.WorkoutHistoryViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.example.gymlocker.data.database.AppDatabase
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.example.gymlocker.ui.components.MuscleGroupDistributionChart

private const val MAX_TEMPLATE_NAME_LENGTH = 40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    navController: NavController,
    viewModel: WorkoutHistoryViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val workoutDetails by viewModel.getWorkoutDetails(workoutId).collectAsState(initial = emptyList())

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    val workout by produceState<com.example.gymlocker.data.entity.Workout?>(initialValue = null, workoutId) {
        value = db.workoutDao().getWorkoutById(workoutId)
    }

    val splitRows by db.performedSetDao()
        .observeMuscleGroupDistributionForWorkout(workoutId)
        .collectAsState(initial = emptyList())

    val totalVolumeKg = remember(workoutDetails) {
        workoutDetails.sumOf { log ->
            log.sets.filter { it.isCompleted }.sumOf { (it.weight * it.reps).toDouble() }
        }
    }

    val prCount by produceState(initialValue = 0, workoutId) {
        val userId = viewModel.activeProfileUserIdOnce()
        value = if (userId == null) 0 else db.performedSetDao().countExercisePRsInWorkout(userId, workoutId)
    }

    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var isCreatingTemplate by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val settings = LocalUserSettings.current
    val unit = settings.weightUnit

    val nameTooLong = templateName.length > MAX_TEMPLATE_NAME_LENGTH
    val nameErrorText = if (nameTooLong) {
        "Name is too long (max $MAX_TEMPLATE_NAME_LENGTH characters)."
    } else null

    if (showCreateTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTemplateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Create Template",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                TextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template name") },
                    singleLine = true,
                    isError = nameErrorText != null,
                    supportingText = {
                        if (nameErrorText != null) Text(nameErrorText)
                        else Text("${templateName.length} / $MAX_TEMPLATE_NAME_LENGTH")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,

                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        errorTextColor = MaterialTheme.colorScheme.onSurface,

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                        errorIndicatorColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary,

                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        errorLabelColor = MaterialTheme.colorScheme.error,

                        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        errorSupportingTextColor = MaterialTheme.colorScheme.error
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (templateName.isNotBlank() && !nameTooLong && !isCreatingTemplate) {
                            isCreatingTemplate = true
                            coroutineScope.launch {
                                viewModel.createTemplateFromWorkout(workoutId, templateName)
                                showCreateTemplateDialog = false
                                templateName = ""
                                isCreatingTemplate = false
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = templateName.isNotBlank() && !nameTooLong && !isCreatingTemplate
                ) {
                    Text(
                        text = if (isCreatingTemplate) "Creating..." else "Create",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateTemplateDialog = false },
                    enabled = !isCreatingTemplate
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Workout Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showCreateTemplateDialog = true }) {
                        Text(
                            text = "Save as Template",
                            color = MaterialTheme.colorScheme.primary
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
        if (workoutDetails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data for this workout.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {

            val durationSeconds = workout?.time ?: 0L
            val durationText = remember(durationSeconds) { formatDuration(durationSeconds) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // 1) Workout name
                item {
                    Text(
                        text = workout?.name ?: "Workout",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // 2) Stats row
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Brug dine eksisterende icons (Timer/Equalizer/EmojiEvents)
                            TopStat(icon = Icons.Default.Timer, label = "Duration", value = durationText)
                            TopStat(
                                icon = Icons.Default.Equalizer,
                                label = "Volume",
                                value = "${totalVolumeKg.toInt()} ${weightUnitLabel(unit)}"
                            )
                            TopStat(icon = Icons.Default.EmojiEvents, label = "PRs", value = prCount.toString())
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                }

                // 3) Split section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Split",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(12.dp))

                            MuscleGroupDistributionChart(
                                rows = splitRows,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // 4) Exercise logs — HER er din store fejl: brug workoutDetails, ikke logs
                items(workoutDetails) { log ->
                    // Exercise title (minimal)
                    Text(
                        text = log.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(8.dp))

                    // Column headers (like Active, but without PREVIOUS + ✓)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "SET",
                            modifier = Modifier.weight(0.6f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            weightUnitLabel(unit).uppercase(),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "REPS",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Set rows (minimal)
                    log.sets.forEach { set ->
                        val alpha = if (set.isCompleted) 1f else 0.45f
                        val shownW = displayWeightFromKg(set.weight.toDouble(), unit)
                        val wText = formatWeight(shownW, decimals = 0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .alpha(alpha),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = set.setNumber.toString(),
                                modifier = Modifier.weight(0.6f),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = wText,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = set.reps.toString(),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    //HorizontalDivider()
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun TopStat(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
