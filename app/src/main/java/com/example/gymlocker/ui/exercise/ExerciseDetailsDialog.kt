package com.example.gymlocker.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.ExerciseStatsUi

@Composable
fun ExerciseDetailsDialog(
    exerciseId: Long,
    exerciseName: String,
    muscleGroupId: Long,
    viewModel: ActiveWorkoutViewModel,
    onDismiss: () -> Unit
) {
    var muscleGroupName by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<ExerciseStatsUi?>(null) }
    val unit = LocalUserSettings.current.weightUnit

    LaunchedEffect(exerciseId, muscleGroupId, unit) {
        muscleGroupName = viewModel.getMuscleGroupName(muscleGroupId)
        stats = viewModel.getExerciseStatsUi(exerciseId, unit)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Muscle group: ${muscleGroupName ?: "Loading..."}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "PR: ${stats?.prText ?: "Loading..."}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last trained: ${stats?.lastTrainedText ?: "Loading..."}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
