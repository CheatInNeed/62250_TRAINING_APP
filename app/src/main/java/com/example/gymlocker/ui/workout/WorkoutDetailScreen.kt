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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector

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
                        if (nameErrorText != null) {
                            Text(
                                text = nameErrorText,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "${templateName.length} / $MAX_TEMPLATE_NAME_LENGTH",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Workout Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showCreateTemplateDialog = true }) {
                        Text(
                            text = "Save as Template",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                item {
                    val durationSeconds = workout?.time ?: 0L
                    val durationText = remember(durationSeconds) { formatDuration(durationSeconds) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TopStat(icon = Icons.Default.Timer, label = "Duration", value = durationText)
                            TopStat(icon = Icons.Default.Equalizer, label = "Volume", value = "${totalVolumeKg.toInt()} kg")
                            TopStat(icon = Icons.Default.EmojiEvents, label = "PRs", value = prCount.toString())
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
                items(workoutDetails) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = log.exerciseName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Header row (Set | Kg | Reps)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Set", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Kg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Reps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            log.sets.forEach { set ->
                                val alpha = if (set.isCompleted) 1f else 0.45f
                                val shownW = displayWeightFromKg(set.weight.toDouble(), unit)
                                val wText = formatWeight(shownW, decimals = 0) // tal uden unit i tabellen

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .alpha(alpha),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(set.setNumber.toString(), style = MaterialTheme.typography.bodyMedium)
                                    Text(wText, style = MaterialTheme.typography.bodyMedium)
                                    Text(set.reps.toString(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
