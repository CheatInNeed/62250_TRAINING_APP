package com.example.gymlocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymlocker.viewmodel.RestTimerState

@Composable
fun RestTimerBar(
    state: RestTimerState,
    onSkip: () -> Unit
) {
    if (!state.isActive) return

    val progress =
        if (state.totalSeconds <= 0) 0f
        else (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = buildString {
                append("Rest")
                state.exerciseName?.let { append(" • ").append(it) }
                append(" • ").append(state.remainingText)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        OutlinedButton(
            onClick = onSkip,
            contentPadding = ButtonDefaults.ContentPadding
        ) { Text("Skip") }
    }

    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp),
        progress = progress
    )
}
