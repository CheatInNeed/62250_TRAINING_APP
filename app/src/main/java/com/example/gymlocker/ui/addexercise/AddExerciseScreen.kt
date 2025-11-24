package com.example.gymlocker.ui.addexercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercises) -> Unit
) {
    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    val allExercises by db.exerciseDao()
        .getAllExercises()
        .collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Text("Add Exercise")
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for an exercise") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val filtered = if (searchQuery.isBlank()) {
                    allExercises
                } else {
                    allExercises.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
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
                            .height(56.dp),  // Flot, afrundet Android-stil
                        shape = RoundedCornerShape(28.dp)
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
    val bgColor = if (selected) Color(0x332196F3) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(bgColor)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (selected) "✔" else "💪",
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(text = exercise.name)
            }
        }
    }
}
