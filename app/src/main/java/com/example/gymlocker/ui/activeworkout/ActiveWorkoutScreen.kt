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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.ui.addexercise.AddExerciseSheet
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.exercise.ExerciseDetailsDialog
import com.example.gymlocker.ui.theme.GymLockerTheme
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.viewmodel.ActiveExerciseState
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.ExerciseSetState
import com.example.gymlocker.ui.components.RestTimerBar
import com.example.gymlocker.ui.components.RestTimerInputDialog
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    viewModel: ActiveWorkoutViewModel
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)
    val hasActiveProfile = activeProfileUserId != null

    var showAddExerciseSheet by remember { mutableStateOf(false) }
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val activeExercises by viewModel.activeExercises.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var detailExercise by remember { mutableStateOf<ActiveExerciseState?>(null) }

    //finish sheet
    var showFinishSummarySheet by remember { mutableStateOf(false) }
    var workoutNameInput by remember { mutableStateOf("") }

    // Numpad navigation state
    var isNumpadVisible by remember { mutableStateOf(false) }
    var cursorPosition by remember { mutableStateOf<CursorPosition?>(null) }
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    //Rest timer
    val restTimer by viewModel.restTimerState.collectAsState()

    val unit = LocalUserSettings.current.weightUnit

    // ✅ Live validation state for name length
    val maxNameLen = ActiveWorkoutViewModel.MAX_WORKOUT_NAME_LENGTH

    val progress by remember(activeExercises) {
        derivedStateOf {
            val totalSets = activeExercises.sumOf { it.sets.size }
            if (totalSets == 0) return@derivedStateOf 0f
            val doneSets = activeExercises.sumOf { ex -> ex.sets.count { it.isDone } }
            doneSets.toFloat() / totalSets.toFloat()
        }
    }

    // Always compute volume in storage units (kg * reps)
    val totalVolumeKg by remember(activeExercises) {
        derivedStateOf {
            activeExercises.sumOf { ex ->
                ex.sets
                    .asSequence()
                    .filter { it.isDone }
                    .sumOf { set ->
                        set.weight.toDouble() * set.reps.toDouble()
                    }
            }
        }
    }

// Convert the total to the selected unit (same scalar factor as weight conversion)
    val totalVolumeShown by remember(totalVolumeKg, unit) {
        derivedStateOf {
            displayWeightFromKg(totalVolumeKg, unit)
        }
    }

// Round for display (matches how you show weights elsewhere)
    val totalVolumeText = remember(totalVolumeShown, unit) {
        totalVolumeShown.roundToInt().toString()
    }


    // ✅ Only start the timer if a profile exists.
    LaunchedEffect(hasActiveProfile) {
        if (hasActiveProfile) {
            viewModel.startTimer()
        } else {
            // Be safe: if user somehow navigates here without a profile,
            // make sure the VM isn't "running" a workout.
            viewModel.stopTimer()
        }
    }

    // Scroll to the selected exercise when cursor position changes
    LaunchedEffect(cursorPosition) {
        cursorPosition?.let { pos ->
            // Scroll to the exercise at the cursor position
            listState.animateScrollToItem(pos.exerciseIndex)
        }
    }

    // Block all dialogs if there is no profile
    if (!hasActiveProfile) {
        showAddExerciseSheet = false
        showDiscardDialog = false
        showFinishSummarySheet = false
        detailExercise = null
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
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }

    val unfinishedMeaningfulSetCount by remember(activeExercises) {
        derivedStateOf {
            activeExercises.sumOf { ex ->
                ex.sets.count { s -> s.weight > 0 && s.reps > 0 && !s.isDone }
            }
        }
    }

    FinishWorkoutSummarySheet(
        visible = showFinishSummarySheet,
        onCancel = {
            showFinishSummarySheet = false
        },

        onFinished = {
            showFinishSummarySheet = false
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        },
        initialWorkoutName = workoutNameInput.ifBlank { defaultWorkoutName() },
        workoutDurationText = viewModel.formatTime(elapsedTime),
        isVeryShortWorkout = elapsedTime < 60,
        unfinishedMeaningfulSetCount = unfinishedMeaningfulSetCount,
        maxNameLength = maxNameLen,
        onMarkUnfinishedAsDone = { viewModel.markAllUnfinishedMeaningfulSetsDone() },
        onSave = { name, markUnfinishedAsDone ->
            if (markUnfinishedAsDone) {
                viewModel.markAllUnfinishedMeaningfulSetsDone()
            }
            viewModel.finishWorkoutWithName(name)
        },
    )

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
                    TextButton(
                        onClick = { showDiscardDialog = true },
                        enabled = hasActiveProfile
                    ) { Text("Discard") }

                    Button(
                        onClick = {
                            scope.launch {
                                if (workoutNameInput.isBlank()) {
                                    workoutNameInput = viewModel.suggestDefaultWorkoutName()
                                }
                                showFinishSummarySheet = true
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = hasActiveProfile
                    ) { Text("Finish") }
                }
            )
        },
        bottomBar = {
            Column {
                // Show reveal handle when numpad is hidden
                if (!isNumpadVisible) {
                    NumpadRevealHandle(
                        onReveal = {
                            isNumpadVisible = true
                            // Auto-select first incomplete set when opening numpad
                            if (cursorPosition == null && activeExercises.isNotEmpty()) {
                                var found = false
                                for ((exIdx, exercise) in activeExercises.withIndex()) {
                                    for ((setIdx, set) in exercise.sets.withIndex()) {
                                        if (!set.isDone) {
                                            cursorPosition = CursorPosition(exIdx, setIdx, FieldType.WEIGHT)
                                            found = true
                                            break
                                        }
                                    }
                                    if (found) break
                                }
                                // If all sets are done, select the first set
                                if (!found) {
                                    cursorPosition = CursorPosition(0, 0, FieldType.WEIGHT)
                                }
                            }
                        }
                    )
                }

                // The numpad bar with animation
                WorkoutNumpadBar(
                    isVisible = isNumpadVisible,
                    onHide = {
                        isNumpadVisible = false
                        cursorPosition = null  // Clear the blue marker when hiding
                    },
                    onNavigateLeft = {
                        if (activeExercises.isEmpty()) return@WorkoutNumpadBar
                        val current = cursorPosition
                        if (current == null) {
                            cursorPosition = CursorPosition(0, 0, FieldType.WEIGHT)
                        } else {
                            cursorPosition = when (current.field) {
                                FieldType.WEIGHT -> current // Already at leftmost
                                FieldType.REPS -> current.copy(field = FieldType.WEIGHT)
                                FieldType.DONE -> current.copy(field = FieldType.REPS)
                            }
                        }
                    },
                    onNavigateRight = {
                        if (activeExercises.isEmpty()) return@WorkoutNumpadBar
                        val current = cursorPosition
                        if (current == null) {
                            cursorPosition = CursorPosition(0, 0, FieldType.WEIGHT)
                        } else {
                            cursorPosition = when (current.field) {
                                FieldType.WEIGHT -> current.copy(field = FieldType.REPS)
                                FieldType.REPS -> current.copy(field = FieldType.DONE)
                                FieldType.DONE -> current // Already at rightmost
                            }
                        }
                    },
                    onNumberClick = { digit ->
                        val current = cursorPosition ?: return@WorkoutNumpadBar
                        val exercise = activeExercises.getOrNull(current.exerciseIndex) ?: return@WorkoutNumpadBar
                        val set = exercise.sets.getOrNull(current.setIndex) ?: return@WorkoutNumpadBar
                        when (current.field) {
                            FieldType.WEIGHT -> {
                                val newValue = set.weight.toString().let {
                                    if (it == "0") digit else it + digit
                                }
                                viewModel.updateSetWeight(exercise.exerciseId, set.setNumber, newValue)
                            }
                            FieldType.REPS -> {
                                val newValue = set.reps.toString().let {
                                    if (it == "0") digit else it + digit
                                }
                                viewModel.updateSetReps(exercise.exerciseId, set.setNumber, newValue)
                            }
                            FieldType.DONE -> { /* Numbers don't apply to checkbox */ }
                        }
                    },
                    onBackspace = {
                        val current = cursorPosition ?: return@WorkoutNumpadBar
                        val exercise = activeExercises.getOrNull(current.exerciseIndex) ?: return@WorkoutNumpadBar
                        val set = exercise.sets.getOrNull(current.setIndex) ?: return@WorkoutNumpadBar
                        when (current.field) {
                            FieldType.WEIGHT -> {
                                val newValue = set.weight.toString().dropLast(1).ifEmpty { "0" }
                                viewModel.updateSetWeight(exercise.exerciseId, set.setNumber, newValue)
                            }
                            FieldType.REPS -> {
                                val newValue = set.reps.toString().dropLast(1).ifEmpty { "0" }
                                viewModel.updateSetReps(exercise.exerciseId, set.setNumber, newValue)
                            }
                            FieldType.DONE -> { /* Backspace doesn't apply to checkbox */ }
                        }
                    },
                    onPlus = {
                        val current = cursorPosition ?: return@WorkoutNumpadBar
                        val exercise = activeExercises.getOrNull(current.exerciseIndex) ?: return@WorkoutNumpadBar
                        val set = exercise.sets.getOrNull(current.setIndex) ?: return@WorkoutNumpadBar
                        when (current.field) {
                            FieldType.WEIGHT -> {
                                viewModel.updateSetWeight(exercise.exerciseId, set.setNumber, (set.weight + 1).toString())
                            }
                            FieldType.REPS -> {
                                viewModel.updateSetReps(exercise.exerciseId, set.setNumber, (set.reps + 1).toString())
                            }
                            FieldType.DONE -> { /* Plus doesn't apply to checkbox */ }
                        }
                    },
                    onMinus = {
                        val current = cursorPosition ?: return@WorkoutNumpadBar
                        val exercise = activeExercises.getOrNull(current.exerciseIndex) ?: return@WorkoutNumpadBar
                        val set = exercise.sets.getOrNull(current.setIndex) ?: return@WorkoutNumpadBar
                        when (current.field) {
                            FieldType.WEIGHT -> {
                                val newValue = (set.weight - 1).coerceAtLeast(0)
                                viewModel.updateSetWeight(exercise.exerciseId, set.setNumber, newValue.toString())
                            }
                            FieldType.REPS -> {
                                val newValue = (set.reps - 1).coerceAtLeast(0)
                                viewModel.updateSetReps(exercise.exerciseId, set.setNumber, newValue.toString())
                            }
                            FieldType.DONE -> { /* Minus doesn't apply to checkbox */ }
                        }
                    },
                    onNext = {
                        if (activeExercises.isEmpty()) return@WorkoutNumpadBar
                        val current = cursorPosition
                        if (current == null) {
                            cursorPosition = CursorPosition(0, 0, FieldType.WEIGHT)
                        } else {
                            when (current.field) {
                                FieldType.WEIGHT -> {
                                    // Move to reps field
                                    cursorPosition = current.copy(field = FieldType.REPS)
                                }
                                FieldType.REPS -> {
                                    // Move to done field
                                    cursorPosition = current.copy(field = FieldType.DONE)
                                }
                                FieldType.DONE -> {
                                    // Mark current set as done
                                    val exercise = activeExercises.getOrNull(current.exerciseIndex) ?: return@WorkoutNumpadBar
                                    val set = exercise.sets.getOrNull(current.setIndex) ?: return@WorkoutNumpadBar
                                    viewModel.toggleSetDone(exercise.exerciseId, set.setNumber, true)

                                    // Move to next set in same exercise, or next exercise
                                    if (current.setIndex < exercise.sets.size - 1) {
                                        // Move to next set in same exercise
                                        cursorPosition = current.copy(
                                            setIndex = current.setIndex + 1,
                                            field = FieldType.WEIGHT
                                        )
                                    } else if (current.exerciseIndex < activeExercises.size - 1) {
                                        // Move to first set of next exercise
                                        cursorPosition = current.copy(
                                            exerciseIndex = current.exerciseIndex + 1,
                                            setIndex = 0,
                                            field = FieldType.WEIGHT
                                        )
                                    }
                                    // If last set of last exercise, stay where we are
                                }
                            }
                        }
                    }
                )
                RestTimerBar(
                    state = restTimer,
                    onSkip = { viewModel.skipRestTimer() }
                )
                ActiveWorkoutBanner(navController, viewModel)

                // Only show AppBottomBar when numpad is hidden
                if (!isNumpadVisible) {
                    AppBottomBar(navController)
                }
            }
        }
    ) { innerPadding ->

        // ✅ No active profile -> friendly gate
        if (!hasActiveProfile) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No profile selected",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create or select a profile before starting a workout.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("profile") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Profile")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { navController.popBackUnlessAtRoot() }) {
                    Text("Back")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                    text = "Total volume: $totalVolumeText ${weightUnitLabel(unit)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

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
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(activeExercises) { exerciseIndex, exercise ->
                        ActiveWorkoutExerciseItem(
                            exercise = exercise,
                            exerciseIndex = exerciseIndex,
                            cursorPosition = cursorPosition,
                            onCursorChange = { newPos -> cursorPosition = newPos },
                            viewModel = viewModel,
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
                            onOpenDetails = { detailExercise = exercise },
                            isNumpadVisible = isNumpadVisible
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
    exerciseIndex: Int,
    cursorPosition: CursorPosition?,
    onCursorChange: (CursorPosition?) -> Unit,
    viewModel: ActiveWorkoutViewModel,
    onAddSet: () -> Unit,
    onMarkAllSetsDone: () -> Unit,
    onWeightChange: (setNumber: Int, newWeight: String) -> Unit,
    onRepsChange: (setNumber: Int, newReps: String) -> Unit,
    onToggleDone: (setNumber: Int, isDone: Boolean) -> Unit,
    onDeleteExercise: () -> Unit = {},
    onDeleteSet: (setNumber: Int) -> Unit = {},
    onOpenDetails: () -> Unit,
    isNumpadVisible: Boolean = false
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var deleteSetsMode by remember { mutableStateOf(false) }

    var showRestDialog by remember { mutableStateOf(false) }
    var restSeconds by remember(exercise.exerciseId) { mutableStateOf<Int?>(null) }


    val unit = LocalUserSettings.current.weightUnit

// Hent saved default rest for denne exercise (per user)
    LaunchedEffect(exercise.exerciseId) {
        restSeconds = viewModel.readDefaultRestSeconds(exerciseId = exercise.exerciseId)

    }

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
            Column(
                modifier = Modifier.weight(1f) // <-- IKKE combinedClickable her
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onOpenDetails() },
                            onLongClick = { onMarkAllSetsDone() }
                        )
                        .padding(vertical = 2.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentWidth()
                        .clickable { showRestDialog = true }
                        .padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Rest timer",
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "  Rest timer: ${restSeconds?.let { viewModel.formatRestSeconds(it) } ?: "Off"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
                    DropdownMenuItem(
                        text = { Text("Delete exercise") },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
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
            Text(weightUnitLabel(unit).uppercase(), modifier = Modifier.weight(0.7f))
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
            exercise.sets.forEachIndexed { setIndex, set ->
                key(set.setNumber) {
                    // Determine if this set row has a selected field
                    val selectedField = if (
                        cursorPosition != null &&
                        cursorPosition.exerciseIndex == exerciseIndex &&
                        cursorPosition.setIndex == setIndex
                    ) cursorPosition.field else null

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
                            onToggleDone = { onToggleDone(set.setNumber, it) },
                            selectedField = selectedField,
                            onFieldSelected = { field ->
                                onCursorChange(CursorPosition(exerciseIndex, setIndex, field))
                            },
                            isNumpadVisible = isNumpadVisible
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
                                onToggleDone = { onToggleDone(set.setNumber, it) },
                                selectedField = selectedField,
                                onFieldSelected = { field ->
                                    onCursorChange(CursorPosition(exerciseIndex, setIndex, field))
                                },
                                isNumpadVisible = isNumpadVisible
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

    if (showRestDialog) {
        RestTimerInputDialog(
            initialSeconds = restSeconds,
            onDismiss = { showRestDialog = false },
            onSave = { seconds ->
                viewModel.setDefaultRestSeconds(
                    exerciseId = exercise.exerciseId,
                    restSeconds = seconds
                )
                restSeconds = seconds
                showRestDialog = false
            },
            onClear = {
                viewModel.setDefaultRestSeconds(exerciseId = exercise.exerciseId, restSeconds = 0)
                restSeconds = null
                showRestDialog = false
            }
        )
    }
}

@Composable
fun ExerciseSetRow(
    set: ExerciseSetState,
    deleteMode: Boolean,
    onDelete: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggleDone: (Boolean) -> Unit,
    selectedField: FieldType? = null,
    onFieldSelected: (FieldType) -> Unit = {},
    isNumpadVisible: Boolean = false
) {
    val alphaContainer = 0.15f
    val selectedAlpha = 0.4f
    val selectedBorderColor = Color(0xFF3A82F7)

    val isWeightPrefilled = set.isWeightPrefilled
    val isRepsPrefilled = set.isRepsPrefilled

    val prefillAlpha = 0.65f
    val normalAlpha = 1.0f

    val rowModifier = when {
        deleteMode -> Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.errorContainer)

        else -> Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    }
    val unit = LocalUserSettings.current.weightUnit

// IMPORTANT: keep an editable string while focused; don't overwrite on every recomposition
    var isWeightFocused by remember { mutableStateOf(false) }
    var weightText by rememberSaveable(set.setNumber, unit) { mutableStateOf("") }

// When NOT editing, sync display from canonical stored kg value
    LaunchedEffect(set.weight, unit) {
        if (!isWeightFocused) {
            weightText =
                if (set.weight == 0) ""
                else formatWeight(displayWeightFromKg(set.weight.toDouble(), unit), decimals = 0)
        }
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
                .padding(horizontal = 4.dp)
                .then(
                    if (selectedField == FieldType.WEIGHT) {
                        Modifier.background(
                            selectedBorderColor.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
                .clickable { onFieldSelected(FieldType.WEIGHT) },
            contentAlignment = Alignment.Center
        ) {
            TextField(
                value = weightText,
                onValueChange = { newText ->
                    // allow empty OR digits only (matches your Int storage)
                    if (newText.isEmpty() || newText.all { it.isDigit() }) {
                        weightText = newText
                        onWeightChange(newText)
                    }
                },
                readOnly = isNumpadVisible,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(.9f)
                    .onFocusChanged { isWeightFocused = it.isFocused },
                placeholder = { Text("–") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(textAlign = TextAlign.Center),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = if (selectedField == FieldType.WEIGHT)
                        selectedBorderColor.copy(alpha = selectedAlpha)
                    else Color.Gray.copy(alpha = alphaContainer),
                    focusedContainerColor = if (selectedField == FieldType.WEIGHT)
                        selectedBorderColor.copy(alpha = selectedAlpha)
                    else Color.Gray.copy(alpha = alphaContainer),
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
                .padding(horizontal = 4.dp)
                .then(
                    if (selectedField == FieldType.REPS) {
                        Modifier.background(
                            selectedBorderColor.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
                .clickable { onFieldSelected(FieldType.REPS) },
            contentAlignment = Alignment.Center
        ) {
            TextField(
                value = if (set.reps == 0) "" else set.reps.toString(),
                onValueChange = onRepsChange,
                readOnly = isNumpadVisible,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(.9f),
                placeholder = { Text("–") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(textAlign = TextAlign.Center),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = if (selectedField == FieldType.REPS)
                        selectedBorderColor.copy(alpha = selectedAlpha)
                    else Color.Gray.copy(alpha = alphaContainer),
                    focusedContainerColor = if (selectedField == FieldType.REPS)
                        selectedBorderColor.copy(alpha = selectedAlpha)
                    else Color.Gray.copy(alpha = alphaContainer),
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

        // Checkbox with selection highlight
        Box(
            modifier = Modifier
                .weight(0.4f)
                .then(
                    if (selectedField == FieldType.DONE) {
                        Modifier.background(
                            selectedBorderColor.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
                .clickable { onFieldSelected(FieldType.DONE) },
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = set.isDone,
                onCheckedChange = onToggleDone
            )
        }

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

private fun defaultWorkoutName(): String {
    // Super simpelt “smart default” uden at ændre ViewModel
    // (du kan senere gøre den smartere med split/push/pull osv.)
    // TODO Make this good
    return "Workout"
}
