package com.example.gymlocker.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.example.gymlocker.viewmodel.WorkoutHistoryViewModel
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    navController: NavController,
    viewModel: WorkoutHistoryViewModel
) {
    val workoutDetails by viewModel.getWorkoutDetails(workoutId).collectAsState(initial = emptyList())
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var isCreatingTemplate by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (templateName.isNotBlank() && !isCreatingTemplate) {
                            isCreatingTemplate = true
                            coroutineScope.launch {
                                viewModel.createTemplateFromWorkout(workoutId, templateName)
                                showCreateTemplateDialog = false
                                templateName = ""
                                isCreatingTemplate = false
                                // Navigate back
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = templateName.isNotBlank() && !isCreatingTemplate
                ) {
                    Text(if (isCreatingTemplate) "Creating..." else "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateTemplateDialog = false },
                    enabled = !isCreatingTemplate
                ) {
                    Text("Cancel")
                }
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Set ${set.setNumber}", style = MaterialTheme.typography.bodyMedium)
                                    Text("${set.weight} kg x ${set.reps}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
