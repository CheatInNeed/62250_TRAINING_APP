package com.example.gymlocker.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.storageKgFromInput
import com.example.gymlocker.util.weightUnitLabel
import com.example.gymlocker.viewmodel.ProfileViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    val activeProfile by profileViewModel.activeProfile.collectAsState()

    val settings = LocalUserSettings.current
    val unit = settings.weightUnit

    var name by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Display text for weight in the chosen unit (UI)
    val initialWeightText =
        if (activeProfile?.weight == 0) ""
        else formatWeight(displayWeightFromKg(activeProfile!!.weight.toDouble(), unit), decimals = 0)

    var weight by remember { mutableStateOf(initialWeightText) }

    // Load current values into editable fields
    LaunchedEffect(activeProfile?.userId, unit) {
        val p = activeProfile ?: return@LaunchedEffect
        name = p.name
        heightText = if (p.height == 0) "" else p.height.toString()

        // Keep weight text in current unit
        weightText =
            if (p.weight == 0) ""
            else formatWeight(displayWeightFromKg(p.weight.toDouble(), unit), decimals = 0)

        weight = weightText
        error = null
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Reset profile?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This resets name/height/weight. Workouts will NOT be deleted.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.resetActiveProfile {
                            showResetConfirm = false
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Edit profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            if (activeProfile == null) {
                Text(
                    text = "No active profile selected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Back") }
                return@Column
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display name") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Height (cm) — leave empty for Not set") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { input ->
                    weightText = input
                    weight = input
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weight (${weightUnitLabel(unit)})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(12.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    // numeric validation: do NOT silently treat "abc" as Not set
                    val hRaw = heightText.trim()
                    val wRaw = weightText.trim()

                    val heightInt: Int? = if (hRaw.isEmpty()) null else hRaw.toIntOrNull()
                    val weightInt: Int? = if (wRaw.isEmpty()) null else wRaw.toIntOrNull()

                    if (hRaw.isNotEmpty() && heightInt == null) {
                        error = "Height must be a number."
                        return@Button
                    }
                    if (wRaw.isNotEmpty() && weightInt == null) {
                        error = "Weight must be a number."
                        return@Button
                    }

                    val wKg = weight.toDoubleOrNull()
                        ?.let { storageKgFromInput(it, unit).roundToInt() }
                        ?: 0

                    profileViewModel.saveProfileEdits(
                        name = name,
                        height = heightInt,
                        weight = wKg,
                        onError = { error = it },
                        onSuccess = { navController.popBackStack() }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Save") }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reset profile",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
