package com.example.gymlocker.ui.activeworkout

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.Sets
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
import com.example.gymlocker.ui.theme.GymLockerTheme
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(navController: NavController, workoutViewModel: WorkoutViewModel = hiltViewModel(), activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()) {
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    val elapsedTime by activeWorkoutViewModel.elapsedTime.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val workoutExercises by workoutViewModel.workoutExercises.collectAsState()
    val sets by workoutViewModel.sets.collectAsState()

    LaunchedEffect(Unit) {
        activeWorkoutViewModel.startTimer()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("Are you sure you want to discard this workout? All progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    activeWorkoutViewModel.discardWorkout()
                    showDiscardDialog = false
                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Workout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showDiscardDialog = true }) {
                        Text("Discard")
                    }
                    Button(
                        onClick = { /* TODO: Finish workout */ },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Finish")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Filled.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { /* TODO: Navigate to profile */ }) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Timer: ${activeWorkoutViewModel.formatTime(elapsedTime)}")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = 0f)
            }
            if (workoutExercises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No exercises added yet.")
                        Text("Start by adding your first exercise.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddExerciseSheet = true }) {
                            Text("Add Exercise")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(workoutExercises) { exercise ->
                        ExerciseItem(exercise = exercise, sets = sets[exercise.exerciseId] ?: emptyList(), onAddSet = { workoutViewModel.addSet(exercise.exerciseId) })
                    }
                }
            }
        }
    }

    if (showAddExerciseSheet) {
        AddExerciseSheet(onDismiss = { showAddExerciseSheet = false }, onAddExercises = { workoutViewModel.addExercisesToWorkout(it) })
    }
}

@Composable
fun ExerciseItem(exercise: Exercises, sets: List<Sets>, onAddSet: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exercise.name)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "SET")
                Text(text = "PREVIOUS")
                Text(text = "KG")
                Text(text = "REPS")
                Text(text = "✓")
            }
            sets.forEach { set ->
                SetItem(set = set)
            }
            Button(onClick = onAddSet) {
                Text("Add Set")
            }
        }
    }
}

@Composable
fun SetItem(set: Sets) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = set.setNumber.toString())
        Text(text = set.previous)
        Text(text = set.kg.toString())
        Text(text = set.reps.toString())
        Checkbox(checked = set.isCompleted, onCheckedChange = { /* TODO */ })
    }
}

@Preview(showBackground = true)
@Composable
fun ActiveWorkoutScreenPreview() {
    GymLockerTheme {
        ActiveWorkoutScreen(rememberNavController())
    }
}
