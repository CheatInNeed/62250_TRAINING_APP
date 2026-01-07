package com.example.gymlocker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.ui.theme.GymLockerTheme
import kotlinx.coroutines.delay

@Composable
private fun rememberSingleClick(cooldownMs: Long = 350L): () -> Boolean {
    var enabled by remember { mutableStateOf(true) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            delay(cooldownMs)
            enabled = true
        }
    }

    return {
        if (!enabled) false
        else {
            enabled = false
            true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(navController: NavController) {
    // One gate for this screen; prevents rapid multi-taps across all buttons here
    val canClick = rememberSingleClick(cooldownMs = 350L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!canClick()) return@IconButton

                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate("home") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!canClick()) return@IconButton
                            navController.navigate("home") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = "Home")
                    }

                    IconButton(
                        onClick = {
                            if (!canClick()) return@IconButton
                            // TODO: Navigate to profile
                            // navController.navigate("profile") { launchSingleTop = true }
                        }
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (!canClick()) return@Button
                    navController.navigate("activeWorkout") {
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Start Empty Workout")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Or choose a routine:")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutScreenPreview() {
    GymLockerTheme {
        WorkoutScreen(rememberNavController())
    }
}
