// FILE: app/src/main/java/com/example/gymlocker/ui/template/CreateTemplateScreen.kt
package com.example.gymlocker.ui.template

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.CreateTemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    navController: NavController,
    viewModel: CreateTemplateViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val templateName by viewModel.templateName.collectAsState()
    val templateNameError by viewModel.templateNameError.collectAsState()
    val templateExercises by viewModel.selectedExercises.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val canSave = templateName.isNotBlank() &&
            templateExercises.isNotEmpty() &&
            !isSaving &&
            templateNameError == null

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
                    TextButton(onClick = { showDiscardDialog = true }) { Text("Discard") }
                    Button(
                        onClick = {
                            if (!canSave) return@Button
                            viewModel.saveTemplate()
                            navController.navigateUp()
                        },
                        enabled = canSave,
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
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = templateName,
                    onValueChange = viewModel::updateTemplateName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = templateNameError != null,
                    placeholder = { Text("e.g. Push Day") },
                    supportingText = {
                        val max = CreateTemplateViewModel.MAX_TEMPLATE_NAME_LENGTH
                        if (templateNameError != null) {
                            Text(templateNameError!!)
                        } else {
                            Text("${templateName.length} / $max")
                        }
                    }
                )
            }

            if (templateExercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No exercises added yet.")
                        Text("Start by adding your first exercise.")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showAddExerciseSheet = true }) { Text("Add Exercise") }
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
                        Spacer(Modifier.height(16.dp))
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showAddExerciseSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add Exercise") }
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

@Composable
fun TemplateExerciseItem(
    exercise: com.example.gymlocker.viewmodel.TemplateExerciseState,
    onAddSet: () -> Unit,
    onWeightChange: (setNumber: Int, newWeight: String) -> Unit,
    onRepsChange: (setNumber: Int, newReps: String) -> Unit,
    onDeleteExercise: () -> Unit = {},
    onDeleteSet: (setNumber: Int) -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var deleteSetsMode by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove exercise?") },
            text = { Text("Are you sure you want to remove this exercise from the template?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExercise()
                    showDeleteConfirm = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleMedium
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete exercise") },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )

                    if (!deleteSetsMode) {
                        DropdownMenuItem(
                            text = { Text("Delete sets") },
                            onClick = {
                                showMenu = false
                                deleteSetsMode = true
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Done deleting sets") },
                            onClick = {
                                showMenu = false
                                deleteSetsMode = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text("SET", modifier = Modifier.weight(0.5f))
            Text("KG", modifier = Modifier.weight(0.7f))
            Text("REPS", modifier = Modifier.weight(0.7f))
        }
        Spacer(modifier = Modifier.height(4.dp))

        exercise.sets.forEach { set ->
            TemplateSetRow(
                set = set,
                deleteMode = deleteSetsMode,
                onDelete = {
                    onDeleteSet(set.setNumber)
                    if (exercise.sets.size <= 2) deleteSetsMode = false
                },
                onWeightChange = { onWeightChange(set.setNumber, it) },
                onRepsChange = { onRepsChange(set.setNumber, it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddSet,
            modifier = Modifier.fillMaxWidth()
        ) { Text("+ Add Set") }
    }
}

@Composable
fun TemplateSetRow(
    set: com.example.gymlocker.viewmodel.TemplateSetState,
    deleteMode: Boolean,
    onDelete: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = set.setNumber.toString(),
            modifier = Modifier.weight(0.5f),
            textAlign = TextAlign.Center
        )

        val alpha = 0.15f

        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            TextField(
                value = if (set.weight == 0f) "" else set.weight.toString(),
                onValueChange = onWeightChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(.9f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Gray.copy(alpha = alpha),
                    focusedContainerColor = Color.Gray.copy(alpha = alpha),
                    disabledContainerColor = Color.Gray.copy(alpha = alpha),
                    errorContainerColor = Color.Gray.copy(alpha = alpha),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                )
            )
        }

        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            TextField(
                value = if (set.reps == 0) "" else set.reps.toString(),
                onValueChange = onRepsChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(.9f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Gray.copy(alpha = alpha),
                    focusedContainerColor = Color.Gray.copy(alpha = alpha),
                    disabledContainerColor = Color.Gray.copy(alpha = alpha),
                    errorContainerColor = Color.Gray.copy(alpha = alpha),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                )
            )
        }

        if (deleteMode) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete set")
            }
        }
    }
}
