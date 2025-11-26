package com.example.gymlocker.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkoutScreen(navController: NavController, newWorkoutViewModel: NewWorkoutViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Workout") },
                actions = {
                    Button(
                        onClick = { navController.navigate("workout_summary") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Text("Finish")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp)
        ) {
            Button(onClick = { navController.navigate("add_exercise") }) {
                Text("Add Exercise")
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(newWorkoutViewModel.exercises.value) { exercise ->
                    ExerciseItem(exercise = exercise, viewModel = newWorkoutViewModel)
                }
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: WorkoutExercise, viewModel: NewWorkoutViewModel) {
    val showSetFields = remember { mutableStateOf(false) }
    val reps = remember { mutableStateOf(exercise.sets.lastOrNull()?.reps ?: "") }
    val weight = remember { mutableStateOf(exercise.sets.lastOrNull()?.weight ?: "") }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(exercise.name)
        exercise.sets.forEach { set ->
            Text("Reps: ${set.reps}, Weight: ${set.weight}")
        }
        Button(onClick = { 
            reps.value = exercise.sets.lastOrNull()?.reps ?: ""
            weight.value = exercise.sets.lastOrNull()?.weight ?: ""
            showSetFields.value = true 
        }) {
            Text("Add Set")
        }
        if (showSetFields.value) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(value = reps.value, onValueChange = { reps.value = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                TextField(value = weight.value, onValueChange = { weight.value = it }, label = { Text("Weight") }, modifier = Modifier.weight(1f))
                Button(onClick = { 
                    viewModel.addSet(exercise, reps.value, weight.value)
                    showSetFields.value = false
                }) {
                    Text("Add")
                }
            }
        }
    }
}
