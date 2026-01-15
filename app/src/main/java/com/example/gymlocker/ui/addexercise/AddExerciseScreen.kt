package com.example.gymlocker.ui.addexercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises

enum class ExerciseFilterMode { BY_NAME, BY_MUSCLE_GROUP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercises) -> Unit
) {
    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    val allExercises by db.exerciseDao()
        .getAllExercises()
        .collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }

    var filterMode by remember { mutableStateOf(ExerciseFilterMode.BY_NAME) }
    var selectedMuscleGroupId by remember { mutableStateOf<Long?>(null) } // null = "All"
    var showMuscleGroupMenu by remember { mutableStateOf(false) }

    val allMuscleGroups by db.muscleGroupDao()
        .getAllMuscleGroups()
        .collectAsState(initial = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    "Add Exercise",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Sort by musclegroup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == ExerciseFilterMode.BY_NAME,
                        onClick = {
                            filterMode = ExerciseFilterMode.BY_NAME
                            selectedMuscleGroupId = null
                            showMuscleGroupMenu = false
                        },
                        label = { Text("By name") }
                    )

                    Box {
                        FilterChip(
                            selected = filterMode == ExerciseFilterMode.BY_MUSCLE_GROUP,
                            onClick = {
                                filterMode = ExerciseFilterMode.BY_MUSCLE_GROUP
                                showMuscleGroupMenu = true
                            },
                            label = {
                                val selectedName = allMuscleGroups
                                    .firstOrNull { it.muscleGroupId == selectedMuscleGroupId }
                                    ?.name

                                Text(selectedName?.let { "Muscle: $it" } ?: "By muscle group")
                            }
                        )

                        DropdownMenu(
                            expanded = showMuscleGroupMenu,
                            onDismissRequest = { showMuscleGroupMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All muscle groups") },
                                onClick = {
                                    selectedMuscleGroupId = null
                                    showMuscleGroupMenu = false
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

                            allMuscleGroups.forEach { mg ->
                                DropdownMenuItem(
                                    text = { Text(mg.name) },
                                    onClick = {
                                        selectedMuscleGroupId = mg.muscleGroupId
                                        showMuscleGroupMenu = false
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

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search for an exercise",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                val filtered = allExercises
                    // 1) altid: search på navn (hvis der er noget i searchQuery)
                    .filter { ex ->
                        searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
                    }
                    // 2) hvis muscle-mode: filtrér også på valgt muskelgruppe
                    .filter { ex ->
                        if (filterMode == ExerciseFilterMode.BY_MUSCLE_GROUP) {
                            selectedMuscleGroupId == null || ex.muscleGroupId == selectedMuscleGroupId
                        } else {
                            true
                        }
                    }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp) // space til knappen
                ) {
                    items(filtered) { exercise ->
                        val isSelected = selectedExercises.contains(exercise.exerciseId)

                        ExerciseListItem(
                            exercise = exercise,
                            selected = isSelected,
                            onClick = {
                                selectedExercises = if (isSelected) {
                                    selectedExercises - exercise.exerciseId
                                } else {
                                    selectedExercises + exercise.exerciseId
                                }
                            }
                        )
                    }
                }
            }

            if (selectedExercises.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 32.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = {
                            val selected = allExercises.filter {
                                selectedExercises.contains(it.exerciseId)
                            }
                            selected.forEach { onExerciseSelected(it) }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        )
                    ) {
                        Text("Add exercise(s)")
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseListItem(
    exercise: Exercises,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (selected) "✔" else "💪",
                modifier = Modifier.padding(end = 16.dp),
                color = content
            )
            Column {
                Text(
                    text = exercise.name,
                    color = content
                )
            }
        }
    }
}
