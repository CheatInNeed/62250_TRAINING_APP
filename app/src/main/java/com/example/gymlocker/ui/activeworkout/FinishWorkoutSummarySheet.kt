package com.example.gymlocker.ui.activeworkout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishWorkoutSummarySheet(
    visible: Boolean,
    onDismiss: () -> Unit,

    // Data
    initialWorkoutName: String,
    workoutDurationText: String,
    isVeryShortWorkout: Boolean,
    unfinishedMeaningfulSetCount: Int,

    // Constraints
    maxNameLength: Int,

    // Actions
    onSave: (workoutName: String, markUnfinishedAsDone: Boolean) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var workoutName by remember(visible) { mutableStateOf(initialWorkoutName) }
    var markUnfinishedAsDone by remember(visible) { mutableStateOf(false) }

    val trimmed = workoutName.trim()
    val tooLong = trimmed.length > maxNameLength
    val blank = trimmed.isBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // Force it to feel like ~70-80% height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 16.dp)
        ) {
            // Header row: Fortryd + Gem
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Resume")
                }

                Button(
                    onClick = {
                        onSave(
                            workoutName.trim(),
                            markUnfinishedAsDone
                        )
                    },
                    enabled = !blank && !tooLong
                ) {
                    Text("Finish Workout", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            /*Text(
                text = "Summary",
                style = MaterialTheme.typography.headlineSmall
            )*/

            Spacer(Modifier.height(16.dp))

            // Name input with smart default
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

            // Status section: unfinished sets -> checkbox choice (default OFF)
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

            // Duration (nudging if under 1 min)
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

            Spacer(Modifier.height(24.dp))
        }
    }
}
