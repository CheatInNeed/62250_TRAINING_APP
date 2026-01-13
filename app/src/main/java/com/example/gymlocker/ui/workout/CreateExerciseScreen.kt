package com.example.gymlocker.ui.workout

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.gymlocker.viewmodel.CreateExerciseViewModel

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

    var muscleGroupExpanded by remember { mutableStateOf(false) }

    val selectedMuscleGroupName = muscleGroups.find { it.muscleGroupId == selectedMuscleGroupId }?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Exercise") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetForm()
                        navController.popBackStack()
                    }) {
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
                singleLine = true
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
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = muscleGroupExpanded,
                    onDismissRequest = { muscleGroupExpanded = false }
                ) {
                    muscleGroups.forEach { muscleGroup ->
                        DropdownMenuItem(
                            text = { Text(muscleGroup.name) },
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
                modifier = Modifier.padding(top = 8.dp)
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
                    singleLine = true
                )

                OutlinedTextField(
                    value = if (startReps == 0) "" else startReps.toString(),
                    onValueChange = { value ->
                        viewModel.updateStartReps(value.toIntOrNull() ?: 0)
                    },
                    label = { Text("Start Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    viewModel.saveExercise {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = viewModel.canSave() && !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Create Exercise")
            }
        }
    }
}

