package com.example.gymlocker.ui.addexercise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymlocker.data.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recentExercises = remember {
        listOf(
            Exercise("Bench Press", "Chest")
        )
    }
    val allExercises = remember {
        listOf(
            Exercise("Squat", "Legs"),
            Exercise("Deadlift", "Back"),
            Exercise("Overhead Press", "Shoulders"),
            Exercise("Barbell Row", "Back"),
            Exercise("Pull-up", "Back"),
            Exercise("Bicep Curl", "Arms"),
            Exercise("Bench Press", "Chest")
        ).distinct()
    }
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Text("Add Exercise")
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for an exercise") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                val isSearching = searchQuery.isNotBlank()

                if (isSearching) {
                    val filteredExercises = (recentExercises + allExercises).distinct().filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                    items(filteredExercises) { exercise ->
                        ExerciseListItem(exercise = exercise)
                    }
                } else {
                    if (recentExercises.isNotEmpty()) {
                        item {
                            Text("Recent Exercises:", modifier = Modifier.padding(bottom = 8.dp))
                        }
                        items(recentExercises) {
                            ExerciseListItem(exercise = it)
                        }
                    }

                    val otherExercises = allExercises.filter { it !in recentExercises }
                    if (otherExercises.isNotEmpty()) {
                        item {
                            Text("All Exercises:", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                        items(otherExercises) {
                            ExerciseListItem(exercise = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseListItem(exercise: Exercise) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Placeholder for icon
            Text(text = "💪", modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(text = exercise.name)
                Text(text = exercise.muscleGroup)
            }
        }
    }
}
