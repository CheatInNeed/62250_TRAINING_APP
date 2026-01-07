package com.example.gymlocker.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymlocker.viewmodel.CreateTemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: CreateTemplateViewModel = viewModel(
        factory = CreateTemplateViewModel.provideFactory(context)
    )

    val templateName by viewModel.templateName.collectAsState()
    val selectedExercises by viewModel.selectedExercises.collectAsState()
    val availableExercises by viewModel.availableExercises.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opret nyt template") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
        ) {
            // Template Name Section
            Text(
                text = "Template Navn",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = templateName,
                onValueChange = { viewModel.updateTemplateName(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Skriv template navn") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selected Exercises Section
            Text(
                text = "Valgte øvelser (${selectedExercises.size})",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (selectedExercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ingen øvelser valgt endnu")
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(selectedExercises) { exercise ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${exercise.startReps} reps × ${exercise.startWeight} kg",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeExercise(exercise.exerciseId) },
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove exercise")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Available Exercises Section
            Text(
                text = "Tilgængelige øvelser",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(availableExercises.filter { available ->
                    !selectedExercises.any { it.exerciseId == available.exerciseId }
                }) { exercise ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.addExercise(exercise) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${exercise.startReps} reps × ${exercise.startWeight} kg",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    viewModel.saveTemplate()
                    navController.navigateUp()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = templateName.isNotBlank() && selectedExercises.isNotEmpty() && !isSaving
            ) {
                Text(if (isSaving) "Gemmer..." else "Gem template")
            }
        }
    }
}

