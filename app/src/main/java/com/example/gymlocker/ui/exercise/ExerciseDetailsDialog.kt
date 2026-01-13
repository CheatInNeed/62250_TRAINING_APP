package com.example.gymlocker.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(exerciseId, muscleGroupId) {
        muscleGroupName = viewModel.getMuscleGroupName(muscleGroupId)
        stats = viewModel.getExerciseStatsUi(exerciseId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Muscle group: ${muscleGroupName ?: "Loading..."}")
                Text("PR: ${stats?.prText ?: "Loading..."}")
                Text("Last trained: ${stats?.lastTrainedText ?: "Loading..."}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
