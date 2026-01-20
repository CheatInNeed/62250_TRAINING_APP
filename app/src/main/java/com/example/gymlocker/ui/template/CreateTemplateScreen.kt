package com.example.gymlocker.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.CreateTemplateViewModel
import com.example.gymlocker.viewmodel.TemplateExerciseState
import com.example.gymlocker.viewmodel.TemplateSetState

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

    val maxLen = CreateTemplateViewModel.MAX_TEMPLATE_NAME_LENGTH

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
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    val unit = LocalUserSettings.current.weightUnit


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Create Template") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showDiscardDialog = true }
                    ) {
                        Text(
                            "Discard",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveTemplate()
                            navController.navigateUp()
                        },
                        enabled = templateName.isNotBlank() &&
                                templateExercises.isNotEmpty() &&
                                !isSaving &&
                                templateNameError == null,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(if (isSaving) "Saving..." else "Save")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Template name",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                TextField(
                    value = templateName,
                    onValueChange = viewModel::updateTemplateName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "e.g. Push Day",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    isError = templateNameError != null || templateName.isBlank(),
                    supportingText = {
                        val txt = when {
                            templateName.isBlank() -> "Please enter a name."
                            templateNameError != null -> (templateNameError ?: "")
                            else -> "${templateName.length} / $maxLen"
                        }
                        Text(
                            txt,
                            color = if (templateNameError != null || templateName.isBlank())
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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

                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        errorLabelColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (templateExercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No exercises added yet.",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Start by adding your first exercise.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showAddExerciseSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text("Add Exercise") }
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
                            onOpenExercise = { exerciseId ->
                                navController.navigate("exerciseDetail/$exerciseId")
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
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
                // ✅ FIX: pass ID (Long), not the entity
                viewModel.addExercise(ex.exerciseId)
                showAddExerciseSheet = false
            }
        )
    }
}

@Composable
fun TemplateExerciseItem(
    exercise: TemplateExerciseState,
    onAddSet: () -> Unit,
    onWeightChange: (setNumber: Int, newWeight: String) -> Unit,
    onRepsChange: (setNumber: Int, newReps: String) -> Unit,
    onOpenExercise: (exerciseId: Long) -> Unit,
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
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenExercise(exercise.exerciseId) }
                        .padding(vertical = 2.dp)
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete exercise") },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurface,
                                trailingIconColor = MaterialTheme.colorScheme.onSurface,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        )

                        if (!deleteSetsMode) {
                            DropdownMenuItem(
                                text = { Text("Delete sets") },
                                onClick = {
                                    showMenu = false
                                    deleteSetsMode = true
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    leadingIconColor = MaterialTheme.colorScheme.onSurface,
                                    trailingIconColor = MaterialTheme.colorScheme.onSurface,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Done deleting sets") },
                                onClick = {
                                    showMenu = false
                                    deleteSetsMode = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    leadingIconColor = MaterialTheme.colorScheme.onSurface,
                                    trailingIconColor = MaterialTheme.colorScheme.onSurface,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SET",
                    modifier = Modifier.weight(0.5f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "KG",
                    modifier = Modifier.weight(0.9f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "REPS",
                    modifier = Modifier.weight(0.9f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("+ Add Set") }
        }
    }
}

@Composable
fun TemplateSetRow(
    set: TemplateSetState,
    deleteMode: Boolean,
    onDelete: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)

    androidx.compose.foundation.layout.Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = set.setNumber.toString(),
            modifier = Modifier.weight(0.5f),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )

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
                modifier = Modifier.fillMaxWidth(0.9f),
                textStyle = TextStyle(textAlign = TextAlign.Center),
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

                    cursorColor = MaterialTheme.colorScheme.primary
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
                modifier = Modifier.fillMaxWidth(0.9f),
                textStyle = TextStyle(textAlign = TextAlign.Center),
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

                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        if (deleteMode) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete set",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
