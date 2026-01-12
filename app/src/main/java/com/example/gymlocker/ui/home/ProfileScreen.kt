package com.example.gymlocker.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.AuthViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    profileViewModel: ProfileViewModel
) {
    // If logged out, send to login
    LaunchedEffect(Unit) {
        authViewModel.isLoggedIn.collectLatest { loggedIn ->
            if (!loggedIn) {
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }

    val activeProfile by profileViewModel.activeProfile.collectAsState()
    val profiles by profileViewModel.profiles.collectAsState()
    val summary by profileViewModel.workoutSummary.collectAsState()

    val hasActiveProfile = activeProfile != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {

            if (!hasActiveProfile) {
                Text(
                    text = "No profile selected",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text("Create a profile to personalize the app (not social).")

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("createProfile") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create profile") }

                Spacer(Modifier.height(16.dp))

                if (profiles.isNotEmpty()) {
                    Text("Your profiles", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profiles) { p ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                                        val h = if (p.height <= 0) "—" else "${p.height} cm"
                                        val w = if (p.weight <= 0) "—" else "${p.weight} kg"
                                        Text("Height: $h  •  Weight: $w")
                                    }
                                    TextButton(onClick = { profileViewModel.setActiveProfile(p.userId) }) {
                                        Text("Select")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            } else {
                val p = activeProfile!!

                Text(p.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))

                val heightText = if (p.height <= 0) "—" else "${p.height} cm"
                val weightText = if (p.weight <= 0) "—" else "${p.weight} kg"

                Text("Height: $heightText", style = MaterialTheme.typography.bodyLarge)
                Text("Weight: $weightText", style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(16.dp))
                Text("Workout summary", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                Text("Total workouts: ${summary.totalWorkouts}")

                if (summary.totalWorkouts == 0) {
                    Text("Most recent: —")
                } else {
                    val name = summary.mostRecentName ?: "—"
                    val date = summary.mostRecentDate ?: "—"
                    Text("Most recent: $name")
                    Text("Date: $date")
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("createProfile") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create another profile")
                }

                Spacer(Modifier.height(8.dp))

                if (profiles.size > 1) {
                    Text("Switch profile", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profiles.filter { it.userId != p.userId }) { other ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(other.name, style = MaterialTheme.typography.bodyLarge)
                                    TextButton(onClick = { profileViewModel.setActiveProfile(other.userId) }) {
                                        Text("Switch")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }

            // Logout (always available)
            Button(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("Log out")
            }
        }
    }
}
