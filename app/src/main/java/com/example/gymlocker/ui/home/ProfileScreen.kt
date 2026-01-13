package com.example.gymlocker.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val activeProfile by profileViewModel.activeProfile.collectAsState()

    // Delete dialog state
    var deleteTargetUserId by remember { mutableStateOf<Long?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Confirm delete dialog
    if (deleteTargetUserId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetUserId = null },
            title = { Text("Delete profile?") },
            text = {
                Text(
                    "This will delete \"$deleteTargetName\" and all workouts/templates linked to it.\n\nThis cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = deleteTargetUserId ?: return@Button
                        profileViewModel.deleteProfile(
                            userIdToDelete = uid,
                            onError = { errorMsg = it },
                            onSuccess = { /* no-op */ }
                        )
                        deleteTargetUserId = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetUserId = null }) { Text("Cancel") }
            }
        )
    }

    // Suggestion: show error dialog
    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Oops") },
            text = { Text(errorMsg ?: "") },
            confirmButton = {
                TextButton(onClick = { errorMsg = null }) { Text("OK") }
            }
        )
    }

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

            // ✅ Fixed header: Active profile details + edit entry
            activeProfile?.let { p ->
                val heightText = if (p.height == 0) "Not set" else "${p.height} cm"
                val weightText = if (p.weight == 0) "Not set" else "${p.weight} kg"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active profile", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(p.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("Height: $heightText  |  Weight: $weightText")

                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { navController.navigate("editProfile") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Edit profile") }
                    }
                }
            }

            Text(
                text = "Choose a profile",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))

            // ✅ Scrollable area: list + empty state + create + logout
            // Use weight(1f) so only this part scrolls.
            if (profiles.isEmpty()) {
                // If empty, we don't need a LazyColumn; keep it simple and centered-ish.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                ) {
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(profiles, key = { it.userId }) { p ->
                        val isActive = p.userId == activeProfileUserId
                        val heightText = if (p.height == 0) "Not set" else "${p.height} cm"
                        val weightText = if (p.weight == 0) "Not set" else "${p.weight} kg"

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
                                    text = "Height: $heightText  |  Weight: $weightText",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            deleteTargetUserId = p.userId
                                            deleteTargetName = p.name
                                        }
                                    ) { Text("Delete") }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate("createProfile") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Create another profile") }

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
        }
    }
}
