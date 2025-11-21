package com.example.gymlocker.ui.addexercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    onDismiss: () -> Unit,
    onAddExercises: (List<Exercises>) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allExercises by viewModel.allExercises.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedExercises by remember { mutableStateOf(listOf<Exercises>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Add Exercise",
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for an exercise") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                val (recentExercises, otherExercises) = allExercises.partition { it.isRecent }

                // Add padding to the bottom to avoid the list being obscured by the floating button
                LazyColumn(modifier = Modifier.padding(bottom = 80.dp)) {
                    val recentToShow = recentExercises.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    val othersToShow = otherExercises.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    if (recentToShow.isNotEmpty()) {
                        item {
                            Text("Recent Exercises", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                        }
                        items(recentToShow) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                isSelected = exercise in selectedExercises,
                                onToggleSelection = {
                                    selectedExercises = if (exercise in selectedExercises) {
                                        selectedExercises - exercise
                                    } else {
                                        selectedExercises + exercise
                                    }
                                }
                            )
                        }
                    }

                    if (othersToShow.isNotEmpty()) {
                        item {
                            Text("All Exercises", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                        items(othersToShow) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                isSelected = exercise in selectedExercises,
                                onToggleSelection = {
                                    selectedExercises = if (exercise in selectedExercises) {
                                        selectedExercises - exercise
                                    } else {
                                        selectedExercises + exercise
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (selectedExercises.isNotEmpty()) {
                Button(
                    onClick = { 
                        onAddExercises(selectedExercises)
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val buttonText = "Add ${selectedExercises.size} Exercise" + if (selectedExercises.size > 1) "s" else ""
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
fun ExerciseListItem(
    exercise: Exercises,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggleSelection() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💪", modifier = Modifier.padding(end = 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.name)
                // Text(text = exercise.muscleGroup) // This can be re-enabled later
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
        }
    }
}
