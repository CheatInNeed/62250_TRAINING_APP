package com.example.gymlocker.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(navController: NavController, newWorkoutViewModel: NewWorkoutViewModel = viewModel()) {
    val workoutName = remember { mutableStateOf("New Workout") }
    val saveState by newWorkoutViewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState) {
            navController.popBackStack("home", inclusive = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Workout Summary") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            TextField(
                value = workoutName.value,
                onValueChange = { workoutName.value = it },
                label = { Text("Workout Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(newWorkoutViewModel.exercises.value) { exercise ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(exercise.name)
                        exercise.sets.forEach { set ->
                            Text("Reps: ${set.reps}, Weight: ${set.weight}")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    newWorkoutViewModel.saveWorkout(workoutName.value)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Workout")
            }
        }
    }
}
