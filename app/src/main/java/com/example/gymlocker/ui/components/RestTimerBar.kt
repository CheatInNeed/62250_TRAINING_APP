package com.example.gymlocker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.viewmodel.RestTimerState

@Composable
fun RestTimerBar(
    state: RestTimerState,
    onSkip: () -> Unit
) {
    if (!state.isActive) return

    val progress =
        if (state.totalSeconds <= 0) 0f
        else (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat())
            .coerceIn(0f, 1f)

    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .metalGloss()
    ) {
        // Top "bar" element (progress) — Peter Standard: metalGloss(TopBarShape)
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .metalGloss(TopBarShape)
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp, bottom = 10.dp),
            progress = progress,
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .metalGloss(BotBarShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.remainingText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Skip")
            }
        }
    }
}
