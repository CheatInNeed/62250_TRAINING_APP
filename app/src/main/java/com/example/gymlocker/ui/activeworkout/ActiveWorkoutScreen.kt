package com.example.gymlocker.ui.activeworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
import com.example.gymlocker.ui.theme.GymLockerTheme
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
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
    var showUnfinishedSetsDialog by remember { mutableStateOf(false) }
    var detailExercise by remember { mutableStateOf<ActiveExerciseState?>(null) }

    // Finish flow dialogs
    var showQuickFinishWarning by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var workoutNameInput by remember { mutableStateOf("") }

    val progress by remember(activeExercises) {
        derivedStateOf {
            val totalSets = activeExercises.sumOf { it.sets.size }
            if (totalSets == 0) return@derivedStateOf 0f
            val doneSets = activeExercises.sumOf { ex -> ex.sets.count { it.isDone } }
            doneSets.toFloat() / totalSets.toFloat()
        }
    }

    val totalVolume by remember(activeExercises) {
        derivedStateOf {
            activeExercises.sumOf { ex ->
                ex.sets
                    .asSequence()
                    .filter { it.isDone } // kun completed sets
                    .sumOf { it.weight.toDouble() * it.reps.toDouble() }
            }
        }
    }

    val totalVolumeText = remember(totalVolume) {
        if (totalVolume % 1.0 == 0.0) totalVolume.toLong().toString()
        else String.format("%.2f", totalVolume)
    }

    LaunchedEffect(Unit) {
        viewModel.startTimer()
    }

    // Discard dialog (original text kept)
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
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Unfinished sets dialog (new feature)
    if (showUnfinishedSetsDialog) {
        AlertDialog(
            onDismissRequest = { showUnfinishedSetsDialog = false },
            title = { Text("Unfinished sets") },
            text = { Text("You have unfinished sets. Do you want to mark these as complete?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markAllUnfinishedMeaningfulSetsDone()
                    showUnfinishedSetsDialog = false

                    // Chain forward
                    workoutNameInput = ""
                    showNameDialog = true
                }) {
                    Text("Mark as complete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnfinishedSetsDialog = false

                    // Chain forward WITHOUT marking
                    workoutNameInput = ""
                    showNameDialog = true
                }) {
                    Text("Keep unfinished")
                }
            }
        )
    }

    // Quick finish warning (RESTORED TEXT)
    if (showQuickFinishWarning) {
        AlertDialog(
            onDismissRequest = { showQuickFinishWarning = false },
            title = { Text("Workout is too short") },
            text = { Text("This workout is under 1 minute. Are you sure you want to finish it?") },
            confirmButton = {
                TextButton(onClick = {
                    showQuickFinishWarning = false

                    // Chain forward
                    if (viewModel.hasUnfinishedMeaningfulSets()) {
                        showUnfinishedSetsDialog = true
                    } else {
                        workoutNameInput = ""
                        showNameDialog = true
                    }
                }) { Text("Finish anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showQuickFinishWarning = false }) { Text("Keep training") }
            }
        )
    }

    // Name dialog (RESTORED TEXT)
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Enter Workout Name") },
            text = {
                OutlinedTextField(
                    value = workoutNameInput,
                    onValueChange = { workoutNameInput = it },
                    label = { Text("Workout name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    viewModel.finishWorkoutWithName(workoutNameInput)
                    navController.popBackUnlessAtRoot()
                    navController.popBackUnlessAtRoot()
                }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Workout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showDiscardDialog = true }) { Text("Discard") }

                    Button(
                        onClick = {
                            // IMPORTANT: only open ONE dialog, then chain from dialog buttons.
                            when {
                                elapsedTime < 60 -> {
                                    showQuickFinishWarning = true
                                }

                                viewModel.hasUnfinishedMeaningfulSets() -> {
                                    showUnfinishedSetsDialog = true
                                }

                                else -> {
                                    workoutNameInput = ""
                                    showNameDialog = true
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Finish") }
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
                    IconButton(onClick = { /* TODO */ }) {
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
            // Timer + progress
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
                    progress = progress
                )
                Text(
                    text = "Total volume: $totalVolumeText kg",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Exercises
            if (activeExercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No exercises added yet.")
                        Text("Start by adding your first exercise.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddExerciseSheet = true }) { Text("Add Exercise") }
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
                            onMarkAllSetsDone = { viewModel.markAllSetsDone(exercise.exerciseId) },
                            onWeightChange = { setNumber, text ->
                                viewModel.updateSetWeight(exercise.exerciseId, setNumber, text)
                            },
                            onRepsChange = { setNumber, text ->
                                viewModel.updateSetReps(exercise.exerciseId, setNumber, text)
                            },
                            onToggleDone = { setNumber, checked ->
                                viewModel.toggleSetDone(exercise.exerciseId, setNumber, checked)
                            },
                            onDeleteExercise = { viewModel.removeExercise(exercise.exerciseId) },
                            onDeleteSet = { setNumber ->
                                viewModel.removeSet(exercise.exerciseId, setNumber)
                            },
                            onOpenDetails = { detailExercise = exercise }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddExerciseSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add Exercise") }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Details dialog
    detailExercise?.let { ex ->
        ExerciseDetailsDialog(
            exerciseId = ex.exerciseId,
            exerciseName = ex.exerciseName,
            muscleGroupId = ex.muscleGroupId,
            viewModel = viewModel,
            onDismiss = { detailExercise = null }
        )

    }

    if (showAddExerciseSheet) {
        AddExerciseSheet(
            onDismiss = { showAddExerciseSheet = false },
            onExerciseSelected = { exercise: Exercises -> viewModel.addExercise(exercise) }
        )
    }
}

@Composable
fun ActiveWorkoutExerciseItem(
    exercise: ActiveExerciseState,
    onAddSet: () -> Unit,
    onMarkAllSetsDone: () -> Unit,
    onWeightChange: (setNumber: Int, newWeight: String) -> Unit,
    onRepsChange: (setNumber: Int, newReps: String) -> Unit,
    onToggleDone: (setNumber: Int, isDone: Boolean) -> Unit,
    onDeleteExercise: () -> Unit = {},
    onDeleteSet: (setNumber: Int) -> Unit = {},
    onOpenDetails: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var deleteSetsMode by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove exercise?") },
            text = { Text("Are you sure you want to remove this exercise from the workout?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExercise()
                    showDeleteConfirm = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.combinedClickable(
                    onClick = { onOpenDetails() },
                    onLongClick = { onMarkAllSetsDone() }
                )
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Mark all sets as complete") },
                        onClick = {
                            showMenu = false
                            onMarkAllSetsDone()
                        }
                    )

                    if (!deleteSetsMode) {
                        DropdownMenuItem(
                            text = { Text("Delete sets") },
                            onClick = {
                                showMenu = false
                                deleteSetsMode = true
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Done deleting sets") },
                            onClick = {
                                showMenu = false
                                deleteSetsMode = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        if (exercise.sets.isEmpty()) {
            Text(
                text = "No sets yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            exercise.sets.forEach { set ->
                key(set.setNumber) {
                    if (deleteSetsMode) {
                        ExerciseSetRow(
                            set = set,
                            deleteMode = true,
                            onDelete = {
                                onDeleteSet(set.setNumber)
                                if (exercise.sets.size <= 1) deleteSetsMode = false
                            },
                            onWeightChange = { onWeightChange(set.setNumber, it) },
                            onRepsChange = { onRepsChange(set.setNumber, it) },
                            onToggleDone = { onToggleDone(set.setNumber, it) }
                        )
                    } else {
                        SwipeableSetRow(
                            enabled = true,
                            isDone = set.isDone,
                            onComplete = { onToggleDone(set.setNumber, true) },
                            onDelete = { onDeleteSet(set.setNumber) }
                        ) {
                            ExerciseSetRow(
                                set = set,
                                deleteMode = false,
                                onDelete = { /* hidden */ },
                                onWeightChange = { onWeightChange(set.setNumber, it) },
                                onRepsChange = { onRepsChange(set.setNumber, it) },
                                onToggleDone = { onToggleDone(set.setNumber, it) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddSet,
            modifier = Modifier.fillMaxWidth()
        ) { Text("+ Add Set") }
    }
}

@Composable
fun ExerciseSetRow(
    set: ExerciseSetState,
    deleteMode: Boolean,
    onDelete: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggleDone: (Boolean) -> Unit
) {
    val alphaContainer = 0.15f

    val isWeightPrefilled = (set.previous != null) && (set.weight != 0)
    val isRepsPrefilled = (set.previous != null) && (set.reps != 0)

    val prefillAlpha = 0.65f
    val normalAlpha = 1.0f

    val rowModifier = when {
        deleteMode -> Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.errorContainer)

        set.isDone -> Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF34C759))

        else -> Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = set.setNumber.toString(),
            modifier = Modifier.weight(0.5f),
            textAlign = TextAlign.Center
        )
        Text(
            text = set.previous ?: "-",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            TextField(
                value = if (set.weight == 0) "" else set.weight.toString(),
                onValueChange = onWeightChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(.9f),
                placeholder = { Text("–") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    focusedContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    disabledContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    errorContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isWeightPrefilled) prefillAlpha else normalAlpha
                    ),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = normalAlpha),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
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
                modifier = Modifier.fillMaxWidth(.9f),
                placeholder = { Text("–") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    focusedContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    disabledContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    errorContainerColor = Color.Gray.copy(alpha = alphaContainer),
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isRepsPrefilled) prefillAlpha else normalAlpha
                    ),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = normalAlpha),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                )
            )
        }

        Checkbox(
            checked = set.isDone,
            onCheckedChange = onToggleDone,
            modifier = Modifier.weight(0.4f)
        )

        if (deleteMode) {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete") }
        }
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

@Composable
fun ExerciseDetailsDialog(
    exerciseId: Long,
    exerciseName: String,
    muscleGroupId: Long,
    viewModel: ActiveWorkoutViewModel,
    onDismiss: () -> Unit
) {
    var muscleGroupName by remember { mutableStateOf<String?>(null) }
    var prText by remember { mutableStateOf<String?>(null) }
    var lastTrainedText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(muscleGroupId) {
        muscleGroupName = viewModel.getMuscleGroupName(muscleGroupId)
    }

    LaunchedEffect(exerciseId) {
        prText = viewModel.getPersonalRecordText(exerciseId)       // "No PR yet" if none
        lastTrainedText = viewModel.getLastTrainedText(exerciseId) // "Never trained" if none
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Muscle group: ${muscleGroupName ?: "Loading..."}")
                Text("PR: ${prText ?: "Loading..."}")
                Text("Last trained: ${lastTrainedText ?: "Loading..."}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

