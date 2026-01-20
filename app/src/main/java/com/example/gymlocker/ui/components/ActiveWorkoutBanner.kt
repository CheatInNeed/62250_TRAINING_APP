package com.example.gymlocker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel

@Composable
fun ActiveWorkoutBanner(
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val isWorkoutInProgress by activeWorkoutViewModel.isWorkoutInProgress.collectAsState()
    val elapsedTime by activeWorkoutViewModel.elapsedTime.collectAsState()

    if (!isWorkoutInProgress) return

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    if (currentRoute == "activeWorkout") return

    val timeText = remember(elapsedTime) { activeWorkoutViewModel.formatTime(elapsedTime) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard workout?") },
            text = { Text("Are you sure you want to discard this workout? All progress will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeWorkoutViewModel.discardWorkout()
                        showDiscardDialog = false
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // Peter Standard: cards use metalGloss + outline, and keep shape consistent
            .metalGloss()
            .padding(vertical = 12.dp, horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: resume (CTA)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
                    // treat as a "bar" surface inside card
                    .metalGloss(TopBarShape)
                    .clickable {
                        navController.navigate("activeWorkout") { launchSingleTop = true }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = "Resume workout"
                )
            }

            Column(
                modifier = Modifier.weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Active workout in progress",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // RIGHT: discard (destructive)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
                    .metalGloss(BotBarShape)
                    .alpha(0.98f)
                    .clickable { showDiscardDialog = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.onError,
                    contentDescription = "Discard workout"
                )
            }
        }
    }
}
