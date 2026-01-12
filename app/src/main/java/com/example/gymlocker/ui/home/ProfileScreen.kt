package com.example.gymlocker.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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

    val profiles by profileViewModel.profiles.collectAsState()
    val activeProfileUserId by profileViewModel.activeProfileUserId.collectAsState()

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
            Text(
                text = "Choose a profile",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))

            if (profiles.isEmpty()) {
                Text(
                    text = "No profiles yet.\nCreate one to get started.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("createProfile") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create Profile") }
            } else {
                profiles.forEach { p ->
                    val isActive = p.userId == activeProfileUserId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { profileViewModel.setActiveProfile(p.userId) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isActive) "✅ ${p.name}" else p.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Height: ${if (p.height == 0) "—" else p.height}  |  Weight: ${if (p.weight == 0) "—" else p.weight}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { navController.navigate("createProfile") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create another profile") }
            }

            Spacer(Modifier.height(20.dp))

            TextButton(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log out")
            }
        }
    }
}
