package com.example.gymlocker.ui.addexercise

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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

            LazyColumn {
                items(filtered) { exercise ->
                    ExerciseListItem(
                        exercise = exercise,
                        onClick = {
                            onExerciseSelected(exercise)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseListItem(
    exercise: Exercises,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(text = "💪", modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(text = exercise.name)
                // Hvis du vil, kan du senere slå muskelgruppen op via muscleGroupId
            }
        }
    }
}
