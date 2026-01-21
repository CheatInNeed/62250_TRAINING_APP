package com.example.gymlocker.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.viewmodel.CreateExerciseViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.gymlocker.data.auth.SessionManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    navController: NavController,
    viewModel: CreateExerciseViewModel
) {
    val exerciseName by viewModel.exerciseName.collectAsState()
    val exerciseNameError by viewModel.exerciseNameError.collectAsState()
    val selectedMuscleGroupId by viewModel.selectedMuscleGroupId.collectAsState()
    val startWeight by viewModel.startWeight.collectAsState()
    val startReps by viewModel.startReps.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val muscleGroups by viewModel.muscleGroups.collectAsState()

    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)


    var muscleGroupExpanded by remember { mutableStateOf(false) }

    val selectedMuscleGroupName =
        muscleGroups.find { it.muscleGroupId == selectedMuscleGroupId }?.name ?: ""

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorLabelColor = MaterialTheme.colorScheme.error,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = { Text("Create Exercise") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.resetForm()
                            navController.popBackUnlessAtRoot()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise Name
            OutlinedTextField(
                value = exerciseName,
                onValueChange = { viewModel.updateExerciseName(it) },
                label = { Text("Exercise Name") },
                placeholder = { Text("e.g., Incline Bench Press") },
                isError = exerciseNameError != null,
                supportingText = exerciseNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors
            )

            // Muscle Group Dropdown
            ExposedDropdownMenuBox(
                expanded = muscleGroupExpanded,
                onExpandedChange = { muscleGroupExpanded = !muscleGroupExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMuscleGroupName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Muscle Group") },
                    placeholder = { Text("Select a muscle group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleGroupExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = fieldColors
                )
                ExposedDropdownMenu(
                    expanded = muscleGroupExpanded,
                    onDismissRequest = { muscleGroupExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    muscleGroups.forEach { muscleGroup ->
                        DropdownMenuItem(
                            text = { Text(muscleGroup.name, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.selectMuscleGroup(muscleGroup.muscleGroupId)
                                muscleGroupExpanded = false
                            }
                        )
                    }
                }
            }

            // Optional: Default starting weight
            Text(
                text = "Default Starting Values (optional)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "These will be used as suggestions when you add this exercise to a workout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = if (startWeight == 0) "" else startWeight.toString(),
                    onValueChange = { value ->
                        viewModel.updateStartWeight(value.toIntOrNull() ?: 0)
                    },
                    label = { Text("Start Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = fieldColors
                )

                OutlinedTextField(
                    value = if (startReps == 0) "" else startReps.toString(),
                    onValueChange = { value ->
                        viewModel.updateStartReps(value.toIntOrNull() ?: 0)
                    },
                    label = { Text("Start Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = fieldColors
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val uid = activeProfileUserId
                    if (uid != null) {
                        viewModel.saveExercise(ownerUserId = uid) {
                            navController.popBackUnlessAtRoot()
                        }
                    } else {
                        // Optional: handle case where there is no active profile
                        // e.g. show a snackbar / toast or just silently ignore
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = viewModel.canSave() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(if (isSaving) "Saving..." else "Create Exercise")
            }
        }
    }
}
