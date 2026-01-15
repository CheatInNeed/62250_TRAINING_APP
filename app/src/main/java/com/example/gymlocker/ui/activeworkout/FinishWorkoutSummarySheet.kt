package com.example.gymlocker.ui.activeworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {

                Text(
                    text = "Workout Summary",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            "Resume",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = workoutName,
                    onValueChange = { workoutName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Workout name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    isError = blank || tooLong,
                    supportingText = {
                        val txt = when {
                            blank -> "Name cannot be empty"
                            tooLong -> "Max $maxNameLength tegn."
                            else -> "${trimmed.length} / $maxNameLength"
                        }
                        Text(
                            txt,
                            color = if (blank || tooLong)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,

                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        errorTextColor = MaterialTheme.colorScheme.onSurface,

                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                        errorBorderColor = MaterialTheme.colorScheme.error,

                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        errorLabelColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))

                if (unfinishedMeaningfulSetCount > 0) {
                    Text(
                        "You have $unfinishedMeaningfulSetCount unfinished sets.",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = markUnfinishedAsDone,
                            onCheckedChange = {
                                markUnfinishedAsDone = it
                                if (it) onMarkUnfinishedAsDone()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Mark all sets as done",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        "No unfinished sets.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
