package com.example.gymlocker.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymlocker.data.database.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun ExerciseMuscleGroupDialog(
    exerciseName: String,
    muscleGroupId: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var muscleGroupName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(muscleGroupId) {
        scope.launch {
            muscleGroupName = db.muscleGroupDao().getNameById(muscleGroupId) ?: "Unknown"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // “scrim” bag dialogen (så man stadig kan se workout bagved)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.66f), // ~2/3 af skærmen
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Divider()

                    Text(
                        text = "Muscle group:",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = muscleGroupName ?: "Loading...",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
