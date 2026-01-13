package com.example.gymlocker.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    val activeProfile by profileViewModel.activeProfile.collectAsState()

    var name by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }

    var error by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Localized strings based on selected language
    val strings = if (selectedLanguage == "Danish") DanishStrings else EnglishStrings

    // Load current values into editable fields
    LaunchedEffect(activeProfile?.userId) {
        val p = activeProfile ?: return@LaunchedEffect
        name = p.name
        heightText = if (p.height == 0) "" else p.height.toString()
        weightText = if (p.weight == 0) "" else p.weight.toString()
        selectedLanguage = p.language
        error = null
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(strings.resetProfileTitle) },
            text = { Text(strings.resetProfileMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.resetActiveProfile {
                            showResetConfirm = false
                            navController.popBackStack()
                        }
                    }
                ) { Text(strings.reset) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(strings.cancel) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.editProfile) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = strings.back)
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
                Text(strings.noActiveProfile)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) { Text(strings.back) }
                return@Column
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.displayName) },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.heightLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.weightLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(20.dp))

            // Language selection
            Text(
                text = strings.language,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(8.dp))

            Column(Modifier.selectableGroup()) {
                // English option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = (selectedLanguage == "English"),
                            onClick = { selectedLanguage = "English" },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedLanguage == "English"),
                        onClick = null
                    )
                    Text(
                        text = strings.englishLanguage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                // Danish option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = (selectedLanguage == "Danish"),
                            onClick = { selectedLanguage = "Danish" },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedLanguage == "Danish"),
                        onClick = null
                    )
                    Text(
                        text = strings.danishLanguage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    // ✅ numeric validation: do NOT silently treat "abc" as Not set
                    val hRaw = heightText.trim()
                    val wRaw = weightText.trim()

                    val heightInt: Int? = if (hRaw.isEmpty()) null else hRaw.toIntOrNull()
                    val weightInt: Int? = if (wRaw.isEmpty()) null else wRaw.toIntOrNull()

                    if (hRaw.isNotEmpty() && heightInt == null) {
                        error = strings.heightMustBeNumber
                        return@Button
                    }
                    if (wRaw.isNotEmpty() && weightInt == null) {
                        error = strings.weightMustBeNumber
                        return@Button
                    }

                    profileViewModel.saveProfileEdits(
                        name = name,
                        height = heightInt,
                        weight = weightInt,
                        language = selectedLanguage,
                        onError = { error = it },
                        onSuccess = { navController.popBackStack() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.save) }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.resetProfile) }
        }
    }
}

// Localization strings
private interface LocalizedStrings {
    val editProfile: String
    val back: String
    val noActiveProfile: String
    val displayName: String
    val heightLabel: String
    val weightLabel: String
    val language: String
    val englishLanguage: String
    val danishLanguage: String
    val save: String
    val resetProfile: String
    val resetProfileTitle: String
    val resetProfileMessage: String
    val reset: String
    val cancel: String
    val heightMustBeNumber: String
    val weightMustBeNumber: String
}

private object EnglishStrings : LocalizedStrings {
    override val editProfile = "Edit profile"
    override val back = "Back"
    override val noActiveProfile = "No active profile selected."
    override val displayName = "Display name"
    override val heightLabel = "Height (cm) — leave empty for Not set"
    override val weightLabel = "Weight (kg) — leave empty for Not set"
    override val language = "Language"
    override val englishLanguage = "English"
    override val danishLanguage = "Danish"
    override val save = "Save"
    override val resetProfile = "Reset profile"
    override val resetProfileTitle = "Reset profile?"
    override val resetProfileMessage = "This resets name/height/weight. Workouts will NOT be deleted."
    override val reset = "Reset"
    override val cancel = "Cancel"
    override val heightMustBeNumber = "Height must be a number."
    override val weightMustBeNumber = "Weight must be a number."
}

private object DanishStrings : LocalizedStrings {
    override val editProfile = "Rediger profil"
    override val back = "Tilbage"
    override val noActiveProfile = "Ingen aktiv profil valgt."
    override val displayName = "Visningsnavn"
    override val heightLabel = "Højde (cm) — lad stå tom for Ikke angivet"
    override val weightLabel = "Vægt (kg) — lad stå tom for Ikke angivet"
    override val language = "Sprog"
    override val englishLanguage = "Engelsk"
    override val danishLanguage = "Dansk"
    override val save = "Gem"
    override val resetProfile = "Nulstil profil"
    override val resetProfileTitle = "Nulstil profil?"
    override val resetProfileMessage = "Dette nulstiller navn/højde/vægt. Træninger slettes IKKE."
    override val reset = "Nulstil"
    override val cancel = "Annuller"
    override val heightMustBeNumber = "Højde skal være et tal."
    override val weightMustBeNumber = "Vægt skal være et tal."
}
