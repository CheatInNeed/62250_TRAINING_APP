package com.example.gymlocker.ui.addexercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(navController: NavController, onExerciseSelected: (String) -> Unit) {
    val allExercises = remember {
        listOf(
            "Squat",
            "Deadlift",
            "Overhead Press",
            "Barbell Row",
            "Pull-up",
            "Bicep Curl",
            "Bench Press"
        ).distinct()
    }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Exercise") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for an exercise") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                val filteredExercises = allExercises.filter {
                    it.contains(searchQuery, ignoreCase = true)
                }
                items(filteredExercises) { exercise ->
                    ExerciseListItem(exercise = exercise) {
                        onExerciseSelected(exercise)
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseListItem(exercise: String, onExerciseClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onExerciseClick() }) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Placeholder for icon
            Text(text = "💪", modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(text = exercise)
            }
        }
    }
}
