package com.example.gymlocker.ui.activeworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishWorkoutSummarySheet(
    visible: Boolean,
    onCancel: () -> Unit,
    onFinished: () -> Unit,

    // Data
    initialWorkoutName: String,
    workoutDurationText: String,
    isVeryShortWorkout: Boolean,
    unfinishedMeaningfulSetCount: Int,

    // Constraints
    maxNameLength: Int,

    // Actions
    onMarkUnfinishedAsDone: () -> Unit,
    onSave: (workoutName: String, markUnfinishedAsDone: Boolean) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var workoutName by remember(visible) { mutableStateOf(initialWorkoutName) }
    var markUnfinishedAsDone by remember(visible) { mutableStateOf(false) }

    // Slide to finish
    var showSuccess by remember(visible) { mutableStateOf(false) }

    val trimmed = workoutName.trim()
    val tooLong = trimmed.length > maxNameLength
    val blank = trimmed.isBlank()
    val canSave = !blank && !tooLong

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Resume")
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = workoutName,
                    onValueChange = { workoutName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Workout name") },
                    singleLine = true,
                    isError = blank || tooLong,
                    supportingText = {
                        when {
                            blank -> Text("Name cannot be empty")
                            tooLong -> Text("Max $maxNameLength tegn.")
                            else -> Text("${trimmed.length} / $maxNameLength")
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))

                if (unfinishedMeaningfulSetCount > 0) {
                    Text("You have $unfinishedMeaningfulSetCount unfinished sets.")
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = markUnfinishedAsDone,
                            onCheckedChange = { markUnfinishedAsDone = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mark all sets as done")
                    }
                } else {
                    Text("No unfinished sets.")
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))

                val durationColor =
                    if (isVeryShortWorkout) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface

                Text(
                    text = workoutDurationText,
                    color = durationColor,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(90.dp))
            }

            SlideToFinish(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .width(260.dp),
                enabled = canSave,
                text = "Slide to finish",
                onFinished = {
                    if (markUnfinishedAsDone) {
                        onMarkUnfinishedAsDone()
                    }

                    onSave(workoutName.trim(), markUnfinishedAsDone)

                    //showSuccess = true
                    //kotlinx.coroutines.delay(1000)
                    //showSuccess = false

                    onSave(workoutName.trim(), markUnfinishedAsDone)
                    onFinished()
                }
            )

            if (showSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✅",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }
        }
    }
}
