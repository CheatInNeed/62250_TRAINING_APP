package com.example.gymlocker.ui.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.viewmodel.CreateTemplateViewModel
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    navController: NavController,
    viewModel: CreateTemplateViewModel
) {
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val templateName by viewModel.templateName.collectAsState()
    val templateExercises by viewModel.selectedExercises.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard template?") },
            text = { Text("Are you sure you want to discard this template?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTemplateName("")
                    showDiscardDialog = false
                    navController.navigateUp()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Template") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showDiscardDialog = true }) {
                        Text("Discard")
                    }
                    Button(
                        onClick = {
                            viewModel.saveTemplate()
                            navController.navigateUp()
                        },
                        enabled = templateName.isNotBlank() && templateExercises.isNotEmpty() && !isSaving,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (isSaving) "Saving..." else "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top: template name (replaces timer/progress)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Template name", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = templateName,
                    onValueChange = viewModel::updateTemplateName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Push Day") }
                )
            }

            // Middle: identical exercise list
            if (templateExercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No exercises added yet.")
                        Text("Start by adding your first exercise.")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showAddExerciseSheet = true }) {
                            Text("Add Exercise")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(templateExercises) { exercise ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column {
                                Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                            }
                            IconButton(
                                onClick = { viewModel.removeExercise(exercise.exerciseId) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.padding(0.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showAddExerciseSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Exercise")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAddExerciseSheet) {
        AddExerciseSheet(
            onDismiss = { showAddExerciseSheet = false },
            onExerciseSelected = { ex ->
                viewModel.addExercise(ex)
                showAddExerciseSheet = false
            }
        )
    }
}


