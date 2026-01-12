package com.example.gymlocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
                Text("Enter mm:ss (e.g. 2:30) or seconds (e.g. 150).")

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    singleLine = true,
                    placeholder = { Text("2:30") },
                    supportingText = { if (error != null) Text(error!!) }
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
        }
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
