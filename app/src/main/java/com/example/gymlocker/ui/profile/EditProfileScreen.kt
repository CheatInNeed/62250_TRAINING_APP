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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.gymlocker.data.entity.WeightUnit
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
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

    // VM/server error message (keep current behavior)
    var error by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Live field errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }

    fun validateName(text: String): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return "Name is required."
        if (clean.length > 40) return "Name must be 1–40 characters."
        return null
    }

    fun validateHeight(text: String): String? {
        val raw = text.trim()
        if (raw.isEmpty()) return null
        val v = raw.toIntOrNull() ?: return "Height must be a whole number."
        if (v !in 1..250) return "Height must be 1–250 cm."
        return null
    }

    fun validateWeight(text: String): String? {
        val raw = text.trim()
        if (raw.isEmpty()) return null

        val v = raw.toDoubleOrNull() ?: return "Weight must be a number."
        if (v <= 0.0) return "Weight must be greater than 0."

        // storage range is ALWAYS 1..400 kg
        val minKg = 1.0
        val maxKg = 400.0

        // convert allowed range to the current input unit
        val (minAllowed, maxAllowed) = when (unit) {
            WeightUnit.KG -> minKg to maxKg
            WeightUnit.LB -> {
                // kg -> lb
                (minKg) to (maxKg * 2.5)
            }
        }

        if (v !in minAllowed..maxAllowed) {
            val label = weightUnitLabel(unit)

            // show nice rounded values for readability
            val minTxt = if (unit == WeightUnit.LB) minAllowed.toInt().toString() else minAllowed.toInt().toString()
            val maxTxt = if (unit == WeightUnit.LB) maxAllowed.toInt().toString() else maxAllowed.toInt().toString()

            return "Weight must be $minTxt–$maxTxt $label."
        }

        return null
    }


    // Inputs: surfaceVariant + onSurfaceVariant labels + outline borders
    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,

        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,

        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,

        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,

        cursorColor = MaterialTheme.colorScheme.primary,

        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error,
        errorCursorColor = MaterialTheme.colorScheme.error
    )

    LaunchedEffect(activeProfile?.userId, unit) {
        val p = activeProfile ?: return@LaunchedEffect

        name = p.name
        heightText = if (p.height == 0) "" else p.height.toString()
        weightText =
            if (p.weight == 0) ""
            else formatWeight(displayWeightFromKg(p.weight.toDouble(), unit), decimals = 0)

        nameError = validateName(name)
        heightError = validateHeight(heightText)
        weightError = validateWeight(weightText)

        error = null
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Reset profile?") },
            text = { Text("This resets name/height/weight. Workouts will NOT be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.resetActiveProfile {
                            showResetConfirm = false
                            navController.popBackUnlessAtRoot()
                        }
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
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
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
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
                    onClick = { navController.popBackUnlessAtRoot() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Back") }
                return@Column
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = validateName(it)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = {
                    nameError?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = tfColors
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = heightText,
                onValueChange = {
                    heightText = it
                    heightError = validateHeight(it)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Height (cm) — leave empty for Not set") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = heightError != null,
                supportingText = {
                    heightError?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = tfColors
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { input ->
                    weightText = input
                    weightError = validateWeight(input)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weight (${weightUnitLabel(unit)})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = weightError != null,
                supportingText = {
                    weightError?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = tfColors
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
                    val nErr = validateName(name)
                    val hErr = validateHeight(heightText)
                    val wErr = validateWeight(weightText)

                    nameError = nErr
                    heightError = hErr
                    weightError = wErr

                    if (nErr != null || hErr != null || wErr != null) return@Button

                    val hRaw = heightText.trim()
                    val wRaw = weightText.trim()

                    val heightInt: Int? = if (hRaw.isEmpty()) null else hRaw.toIntOrNull()
                    val wKg = if (wRaw.isEmpty()) 0 else storageKgFromInput(wRaw.toDouble(), unit).roundToInt()

                    profileViewModel.saveProfileEdits(
                        name = name.trim(),
                        height = heightInt,
                        weight = wKg,
                        onError = { error = it },
                        onSuccess = { navController.popBackUnlessAtRoot() }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset profile")
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = { navController.navigate("createProfile") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create new profile")
            }
        }
    }
}
