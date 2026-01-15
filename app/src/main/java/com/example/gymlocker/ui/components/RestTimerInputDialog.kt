package com.example.gymlocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp

@Composable
fun RestTimerInputDialog(
    initialSeconds: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onClear: () -> Unit
) {
    var input by remember {
        mutableStateOf(initialSeconds?.let { formatMmSs(it) } ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set rest timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter mm:ss (e.g. 2:30) or seconds (e.g. 150).",
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "2:30",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    supportingText = {
                        if (error != null) {
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    isError = error != null,
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val seconds = parseRestSeconds(input)
                if (seconds == null) {
                    error = "Invalid format. Use mm:ss or seconds."
                } else {
                    onSave(seconds)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text("Clear") }
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

private fun parseRestSeconds(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    if (trimmed.contains(":")) {
        val parts = trimmed.split(":")
        if (parts.size != 2) return null
        val m = parts[0].toIntOrNull() ?: return null
        val s = parts[1].toIntOrNull() ?: return null
        if (m < 0 || s < 0 || s > 59) return null
        return m * 60 + s
    }

    val sec = trimmed.toIntOrNull() ?: return null
    if (sec < 0) return null
    return sec
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = (totalSeconds / 60).coerceAtLeast(0)
    val s = (totalSeconds % 60).coerceAtLeast(0)
    return "%d:%02d".format(m, s)
}
