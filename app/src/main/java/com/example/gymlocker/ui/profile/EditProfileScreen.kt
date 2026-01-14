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
import com.example.gymlocker.data.auth.HeightUnit
import com.example.gymlocker.data.auth.WeightUnit
import com.example.gymlocker.viewmodel.ProfileViewModel
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.storageKgFromInput
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    val activeProfile by profileViewModel.activeProfile.collectAsState()

    // ✅ unit settings
    val weightUnit by profileViewModel.weightUnit.collectAsState(initial = WeightUnit.KG)
    val heightUnit by profileViewModel.heightUnit.collectAsState(initial = HeightUnit.CM)

    val settings = LocalUserSettings.current
    val unit = settings.weightUnit

    var name by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") } // cm OR ft-in text
    var weightText by remember { mutableStateOf("") } // kg OR lb text

    var error by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    /**
     * ✅ Load canonical values into UI fields whenever:
     * - active profile changes
     * - unit setting changes
     *
     * This makes it behave like a settings change:
     * switching units updates what you edit.
     */
    LaunchedEffect(activeProfile?.userId, heightUnit, weightUnit) {

    val initialWeightText =
        if (activeProfile?.weight == 0) ""
        else formatWeight(displayWeightFromKg(activeProfile!!.weight.toDouble(), unit), decimals = 0)

    var weight by remember { mutableStateOf(initialWeightText) }

    // Load current values into editable fields
    LaunchedEffect(activeProfile?.userId) {
        val p = activeProfile ?: return@LaunchedEffect
        name = p.name

        heightText = when (heightUnit) {
            HeightUnit.CM -> if (p.height == 0) "" else p.height.toString()
            HeightUnit.FT_IN -> if (p.height == 0) "" else cmToFtInText(p.height)
        }

        weightText = when (weightUnit) {
            WeightUnit.KG -> if (p.weight == 0) "" else p.weight.toString()
            WeightUnit.LB -> if (p.weight == 0) "" else kgToLbText(p.weight)
        }

        error = null
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset profile?") },
            text = { Text("This resets name/height/weight. Workouts will NOT be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.resetActiveProfile {
                            showResetConfirm = false
                            navController.popBackStack()
                        }
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    val heightLabel = when (heightUnit) {
        HeightUnit.CM -> "Height (cm) — leave empty for Not set"
        HeightUnit.FT_IN -> "Height (ft-in) e.g. 5' 11\" — leave empty for Not set"
    }

    val weightLabel = when (weightUnit) {
        WeightUnit.KG -> "Weight (kg) — leave empty for Not set"
        WeightUnit.LB -> "Weight (lb) — leave empty for Not set"
    }

    // input keyboard types (ft-in still uses text)
    val heightKeyboard = when (heightUnit) {
        HeightUnit.CM -> KeyboardOptions(keyboardType = KeyboardType.Number)
        HeightUnit.FT_IN -> KeyboardOptions(keyboardType = KeyboardType.Text)
    }
    val weightKeyboard = when (weightUnit) {
        WeightUnit.KG -> KeyboardOptions(keyboardType = KeyboardType.Number)
        WeightUnit.LB -> KeyboardOptions(keyboardType = KeyboardType.Number)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                Text("No active profile selected.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Back") }
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
                label = { Text(heightLabel) },
                singleLine = true,
                keyboardOptions = heightKeyboard
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(weightLabel) },
                label = { Text("Weight (${weightUnitLabel(unit)})") },
                singleLine = true,
                keyboardOptions = weightKeyboard
            )

            Spacer(Modifier.height(12.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val hRaw = heightText.trim()
                    val wRaw = weightText.trim()

                    // ✅ parse height based on unit
                    val heightCm: Int? = if (hRaw.isEmpty()) {
                        null
                    } else {
                        when (heightUnit) {
                            HeightUnit.CM -> hRaw.toIntOrNull()
                            HeightUnit.FT_IN -> parseFtInToCm(hRaw)
                        }
                    }

                    // ✅ parse weight based on unit
                    val weightKg: Int? = if (wRaw.isEmpty()) {
                        null
                    } else {
                        when (weightUnit) {
                            WeightUnit.KG -> wRaw.toIntOrNull()
                            WeightUnit.LB -> {
                                val lb = wRaw.toDoubleOrNull()
                                if (lb == null) null else lbToKg(lb)
                            }
                        }
                    }

                    // strict validation (don’t silently treat invalid input as Not set)
                    if (hRaw.isNotEmpty() && heightCm == null) {
                        error = when (heightUnit) {
                            HeightUnit.CM -> "Height must be a number."
                            HeightUnit.FT_IN -> "Height must be like 5' 11\" (or 5 11, or 71)."
                        }
                        return@Button
                    }
                    if (wRaw.isNotEmpty() && weightKg == null) {
                        error = when (weightUnit) {
                            WeightUnit.KG -> "Weight must be a number."
                            WeightUnit.LB -> "Weight must be a number."
                        }
                        return@Button
                    }
                    val wKg = weight.toDoubleOrNull()
                        ?.let { storageKgFromInput(it, unit).roundToInt() }
                        ?: 0

                    profileViewModel.saveProfileEdits(
                        name = name,
                        height = heightInt,
                        weight = wKg,
                        height = heightCm,
                        weight = weightKg,
                        onError = { error = it },
                        onSuccess = { navController.popBackStack() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reset profile") }
        }
    }
}

/** ===== Helpers (canonical store = cm + kg) ===== */

private fun kgToLbText(kg: Int): String {
    val lb = kg * 2.2046226218
    return String.format(Locale.US, "%.1f", lb).removeSuffix(".0")
}

private fun lbToKg(lb: Double): Int {
    return (lb / 2.2046226218).roundToInt()
}

private fun cmToFtInText(cm: Int): String {
    val totalInches = cm / 2.54
    var feet = floor(totalInches / 12.0).toInt()
    var inches = (totalInches - feet * 12.0).roundToInt()
    if (inches == 12) { feet += 1; inches = 0 }
    return "$feet' $inches\""
}

/**
 * Accepts:
 * - 5'11
 * - 5' 11
 * - 5 11
 * - 5' 11"
 * - 71 (treated as inches)
 */
private fun parseFtInToCm(text: String): Int? {
    val t = text.trim()
    if (t.isBlank()) return 0

    val cleaned = t
        .replace("\"", "")
        .replace("’", "'")
        .replace("ft", "'")
        .replace("in", "")
        .trim()

    // 5'11
    val regex = Regex("""^\s*(\d+)\s*'\s*(\d+)?\s*$""")
    val m = regex.find(cleaned)
    if (m != null) {
        val feet = m.groupValues[1].toInt()
        val inches = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: 0
        val totalIn = feet * 12 + inches
        return (totalIn * 2.54).roundToInt()
    }

    // "5 11"
    val parts = cleaned.split(" ").filter { it.isNotBlank() }
    if (parts.size == 2) {
        val feet = parts[0].toIntOrNull() ?: return null
        val inches = parts[1].toIntOrNull() ?: return null
        val totalIn = feet * 12 + inches
        return (totalIn * 2.54).roundToInt()
    }

    // inches only (e.g. "71")
    val inchesOnly = cleaned.toIntOrNull()
    if (inchesOnly != null) {
        return (inchesOnly * 2.54).roundToInt()
    }

    return null
}
