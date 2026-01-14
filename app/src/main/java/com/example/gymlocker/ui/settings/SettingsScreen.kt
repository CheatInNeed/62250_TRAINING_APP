package com.example.gymlocker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.entity.AppTheme
import com.example.gymlocker.data.entity.WeightUnit
import com.example.gymlocker.data.repo.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val repo = remember { SettingsRepository(context.applicationContext) }

    val activeUserId by session.activeProfileUserId.collectAsState(initial = null)
    val settingsOrNull by repo.activeSettings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    // Guard: if no active profile, block editing
    if (activeUserId == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No active profile selected.")
            }
        }
        return
    }

    val currentSettings = settingsOrNull ?: return // waits one frame until repo emits

    // Local UI selection
    var selectedUnit by remember(currentSettings.userId, currentSettings.weightUnit) {
        mutableStateOf(currentSettings.weightUnit)
    }

    var forceDarkMode by remember(currentSettings.userId, currentSettings.forceDarkMode) {
        mutableStateOf(currentSettings.forceDarkMode)
    }

    var restTimerEnabled by remember(currentSettings.userId, currentSettings.restTimerEnabled) {
        mutableStateOf(currentSettings.restTimerEnabled)
    }
    val themeOptions = listOf(
        AppTheme.DEFAULT to "Default (matches system)",
        AppTheme.RED to "Red",
        AppTheme.BLUE to "Blue",
        AppTheme.GREEN to "Green"
    )

    var selectedTheme by remember(currentSettings.userId, currentSettings.appTheme) {
        mutableStateOf(currentSettings.appTheme)
    }

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Units ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Units", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    UnitChoiceRow(
                        title = "Metric",
                        subtitle = "Kilograms (kg)",
                        selected = selectedUnit == WeightUnit.KG,
                        onSelect = { selectedUnit = WeightUnit.KG }
                    )

                    Spacer(Modifier.height(8.dp))

                    UnitChoiceRow(
                        title = "Imperial",
                        subtitle = "Pounds (lb)",
                        selected = selectedUnit == WeightUnit.LB,
                        onSelect = { selectedUnit = WeightUnit.LB }
                    )
                }
            }

            // --- Rest timer ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable rest timer", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (restTimerEnabled) "Rest countdown shows after completing sets"
                            else "No rest countdown will appear or start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Switch(
                        checked = restTimerEnabled,
                        onCheckedChange = { restTimerEnabled = it }
                    )
                }
            }

            // --- Dark mode ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Force dark mode", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Always use dark theme (ignores system).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Switch(
                        checked = forceDarkMode,
                        onCheckedChange = { forceDarkMode = it }
                    )
                }
            }
            Column(Modifier.padding(16.dp)) {
                Text("Choose theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = themeOptions.first { it.first == selectedTheme }.second,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        themeOptions.forEach { (theme, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedTheme = theme
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        repo.updateForActive {
                            it.copy(
                                weightUnit = selectedUnit,
                                restTimerEnabled = restTimerEnabled,
                                forceDarkMode = forceDarkMode,
                                appTheme = selectedTheme
                            )
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}



@Composable
private fun UnitChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}
