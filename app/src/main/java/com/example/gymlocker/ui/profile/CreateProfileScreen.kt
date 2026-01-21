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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.util.storageKgFromInput
import com.example.gymlocker.util.weightUnitLabel
import com.example.gymlocker.viewmodel.ProfileViewModel
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.data.entity.WeightUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val authId by profileViewModel.authId.collectAsState(initial = null)

    // 🚫 Block profile creation when there is an active workout
    val activeExercises by activeWorkoutViewModel.activeExercises.collectAsState()
    val hasActiveWorkout = activeExercises.isNotEmpty()

    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    val settings = LocalUserSettings.current
    val unit = settings.weightUnit

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
                // TODO: use real kg->lb conversion if/when added
                (minKg) to (maxKg * 25)
            }
        }

        if (v !in minAllowed..maxAllowed) {
            val label = weightUnitLabel(unit)

            val minTxt = minAllowed.toInt().toString()
            val maxTxt = maxAllowed.toInt().toString()

            return "Weight must be $minTxt–$maxTxt $label."
        }

        return null
    }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = { Text("Create Profile") },
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
        // 🔒 If there is an active workout, show a guard instead of the form
        if (hasActiveWorkout) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Active workout in progress",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You can't create a new profile while a workout is active. Finish or discard your current workout first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { navController.popBackUnlessAtRoot() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("OK, go back")
                }
            }
            return@Scaffold
        }

        // ✅ Normal "create profile" form when no active workout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = validateName(it)
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                supportingText = {
                    nameError?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = tfColors
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = height,
                onValueChange = {
                    height = it
                    heightError = validateHeight(it)
                },
                label = { Text("Height (cm)") },
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = {
                    weight = it
                    weightError = validateWeight(it)
                },
                label = { Text("Weight (${weightUnitLabel(unit)})") },
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    val cleanName = name.trim()

                    val nErr = validateName(cleanName)
                    val hErr = validateHeight(height)
                    val wErr = validateWeight(weight)

                    nameError = nErr
                    heightError = hErr
                    weightError = wErr

                    if (authId == null) return@Button
                    if (nErr != null || hErr != null || wErr != null) return@Button

                    val hRaw = height.trim()
                    val wRaw = weight.trim()

                    val h = if (hRaw.isEmpty()) 0 else hRaw.toInt()
                    val wKg = if (wRaw.isEmpty()) 0 else storageKgFromInput(wRaw.toDouble(), unit).roundToInt()

                    profileViewModel.createProfile(
                        name = cleanName,
                        height = h,
                        weight = wKg
                    ) {
                        navController.navigate("profile") {
                            popUpTo("createProfile") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authId != null && name.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create & Select")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { navController.popBackUnlessAtRoot() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
