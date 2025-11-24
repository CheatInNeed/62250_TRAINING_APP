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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
import com.example.gymlocker.ui.theme.GymLockerTheme
import com.example.gymlocker.viewmodel.ActiveExerciseState
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.ExerciseSetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    viewModel: ActiveWorkoutViewModel
) {
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val activeExercises by viewModel.activeExercises.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startTimer()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("Are you sure you want to discard this workout? All progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardWorkout()
                    showDiscardDialog = false
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
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
                        onClick = { /* TODO: Finish workout & maybe mark as completed */ },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top: timer + progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Timer: ${viewModel.formatTime(elapsedTime)}")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = 0f // du kan senere bruge dette til fx volume/progress
                )
            }

            // Midten: Liste af øvelser
            if (activeExercises.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(activeExercises) { exercise ->
                        ActiveWorkoutExerciseItem(
                            exercise = exercise,
                            onAddSet = { viewModel.addSet(exercise.exerciseId) },
                            onWeightChange = { setNumber, text ->
                                val value = text.toIntOrNull() ?: 0
                                viewModel.updateSetWeight(exercise.exerciseId, setNumber, value)
                            },
                            onRepsChange = { setNumber, text ->
                                val value = text.toIntOrNull() ?: 0
                                viewModel.updateSetReps(exercise.exerciseId, setNumber, value)
                            },
                            onToggleDone = { setNumber, checked ->
                                viewModel.toggleSetDone(exercise.exerciseId, setNumber, checked)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddExerciseSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Exercise")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAddExerciseSheet) {
        AddExerciseSheet(
            onDismiss = { showAddExerciseSheet = false },
            onExerciseSelected = { exercise: Exercises ->
                viewModel.addExercise(exercise)
            }
        )
    }
}

@Composable
fun ActiveWorkoutExerciseItem(
    exercise: ActiveExerciseState,
    onAddSet: () -> Unit,
    onWeightChange: (setNumber: Int, newWeight: String) -> Unit,
    onRepsChange: (setNumber: Int, newReps: String) -> Unit,
    onToggleDone: (setNumber: Int, isDone: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = exercise.exerciseName,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Header-rækken: SET / PREVIOUS / KG / REPS / Done
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SET", modifier = Modifier.weight(0.5f))
            Text("PREVIOUS", modifier = Modifier.weight(1f))
            Text("KG", modifier = Modifier.weight(0.7f))
            Text("REPS", modifier = Modifier.weight(0.7f))
            Text("✓", modifier = Modifier.weight(0.4f))
        }
        Spacer(modifier = Modifier.height(4.dp))

        exercise.sets.forEach { set ->
            ExerciseSetRow(
                set = set,
                onWeightChange = { onWeightChange(set.setNumber, it) },
                onRepsChange = { onRepsChange(set.setNumber, it) },
                onToggleDone = { onToggleDone(set.setNumber, it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddSet,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Set")
        }
    }
}

@Composable
fun ExerciseSetRow(
    set: ExerciseSetState,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggleDone: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = set.setNumber.toString(),
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = set.previous ?: "-",
            modifier = Modifier.weight(1f)
        )
        TextField(
            value = if (set.weight == 0) "" else set.weight.toString(),
            onValueChange = onWeightChange,
            modifier = Modifier.weight(0.7f),
            singleLine = true
        )
        TextField(
            value = if (set.reps == 0) "" else set.reps.toString(),
            onValueChange = onRepsChange,
            modifier = Modifier.weight(0.7f),
            singleLine = true
        )
        Checkbox(
            checked = set.isDone,
            onCheckedChange = onToggleDone,
            modifier = Modifier.weight(0.4f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ActiveWorkoutScreenPreview() {
    GymLockerTheme {
        ActiveWorkoutScreen(
            navController = rememberNavController(),
            viewModel = ActiveWorkoutViewModel.provideFactory(
                context = androidx.compose.ui.platform.LocalContext.current
            ).create(ActiveWorkoutViewModel::class.java)
        )
    }
}
