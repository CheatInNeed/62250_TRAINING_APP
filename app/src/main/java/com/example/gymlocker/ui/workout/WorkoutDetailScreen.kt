package com.example.gymlocker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.WorkoutHistoryViewModel
import kotlinx.coroutines.launch
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel

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
            title = { Text("Create Template") },
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
                    modifier = Modifier.fillMaxWidth()
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
                    Text(if (isCreatingTemplate) "Creating..." else "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateTemplateDialog = false },
                    enabled = !isCreatingTemplate
                ) { Text("Cancel") }
            }
        )
    }

    Scaffold(
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
                        Text("Save as Template")
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
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No data for this workout.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                items(workoutDetails) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = log.exerciseName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            log.sets.forEach { set ->
                                val shownW = displayWeightFromKg(set.weight.toDouble(), unit)
                                val wText = "${formatWeight(shownW, decimals = 0)} ${weightUnitLabel(unit)}"
                                val line = if (set.isCompleted) "$wText x ${set.reps}" else "$wText x ${set.reps} (skipped)"


                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Set ${set.setNumber}", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
