package com.example.gymlocker.ui.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.template.WorkoutTemplateWithExercises
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    templateId: Long,
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val templateState = remember { mutableStateOf<WorkoutTemplateWithExercises?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(templateId) {
        templateState.value = activeWorkoutViewModel.getTemplateWithExercises(templateId)
        isLoading.value = false
    }

    val template = templateState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        } else if (template != null) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Template header with play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = template.template.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = template.template.date,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(
                        onClick = {
                            val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            activeWorkoutViewModel.startWorkoutFromTemplate(
                                templateId = templateId,
                                userId = 1L,
                                date = dateString
                            )
                            navController.navigate("activeWorkout")
                        }
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start workout")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Exercises list
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(template.exercises) { exerciseWithSets ->
                        ExerciseCard(exerciseWithSets)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Template not found")
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exerciseWithSets: com.example.gymlocker.data.entity.template.TemplateExerciseWithSets,
    exerciseName: String = "Unknown Exercise"
) {
    val context = LocalContext.current
    val fetchedExerciseName = remember { mutableStateOf(exerciseName) }

    LaunchedEffect(exerciseWithSets.templateExercise.exerciseId) {
        val db = AppDatabase.getDatabase(context)
        val exercise = db.exerciseDao().getById(exerciseWithSets.templateExercise.exerciseId)
        fetchedExerciseName.value = exercise?.name ?: "Unknown Exercise"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = fetchedExerciseName.value,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sets",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            exerciseWithSets.sets.forEach { set ->
                Text(
                    text = "Set ${set.setNumber}: ${set.weight}kg × ${set.reps} reps",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

