package com.example.gymlocker.ui.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.EditTemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTemplateScreen(
    templateId: Long,
    navController: NavController,
    viewModel: EditTemplateViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    var showAddExerciseSheet by remember { mutableStateOf(false) }

    val templateName by viewModel.templateName.collectAsState()
    val templateNameError by viewModel.templateNameError.collectAsState()
    val templateExercises by viewModel.selectedExercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val maxLen = EditTemplateViewModel.MAX_TEMPLATE_NAME_LENGTH

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Template") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveTemplate()
                            // Set flag to reload template on detail screen
                            navController.previousBackStackEntry?.savedStateHandle?.set("shouldReloadTemplate", true)
                            // Give a moment for save to complete, then go back
                            navController.popBackStack()
                        },
                        enabled = templateName.isNotBlank() &&
                                templateExercises.isNotEmpty() &&
                                !isSaving &&
                                templateNameError == null &&
                                !isLoading,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (isSaving) "Saving..." else "Save")
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Template name", style = MaterialTheme.typography.labelMedium)
                    TextField(
                        value = templateName,
                        onValueChange = viewModel::updateTemplateName,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g. Push Day") }
                    )
                    if (templateNameError != null) {
                        Text(templateNameError ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }

                if (templateExercises.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No exercises added yet.")
                            Text("Start by adding your first exercise.")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(templateExercises) { exercise ->
                            TemplateExerciseItem(
                                exercise = exercise,
                                onAddSet = { viewModel.addSet(exercise.exerciseId) },
                                onWeightChange = { setNumber, text ->
                                    viewModel.updateSetWeight(exercise.exerciseId, setNumber, text)
                                },
                                onRepsChange = { setNumber, text ->
                                    viewModel.updateSetReps(exercise.exerciseId, setNumber, text)
                                },
                                onDeleteExercise = { viewModel.removeExercise(exercise.exerciseId) },
                                onDeleteSet = { setNumber ->
                                    viewModel.removeSet(exercise.exerciseId, setNumber)
                                }
                            )
                        }

                        item {
                            Button(
                                onClick = { showAddExerciseSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text("Add Exercise")
                            }
                        }
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

