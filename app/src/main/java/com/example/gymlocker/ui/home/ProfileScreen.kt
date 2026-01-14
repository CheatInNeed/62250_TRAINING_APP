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
import com.example.gymlocker.data.auth.HeightUnit
import com.example.gymlocker.data.auth.WeightUnit
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.AuthViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

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

    // ✅ IMPORTANT: give initial values so types are stable
    val weightUnit by profileViewModel.weightUnit.collectAsState(initial = WeightUnit.KG)
    val heightUnit by profileViewModel.heightUnit.collectAsState(initial = HeightUnit.CM)

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

            // ✅ Active profile card
            activeProfile?.let { p ->
                val heightText = formatHeight(p.height, heightUnit)
                val weightText = formatWeight(p.weight, weightUnit)

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

                // ✅ Units selector card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Units", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))

                        Text("Weight", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { profileViewModel.setWeightUnit(WeightUnit.KG) },
                                enabled = weightUnit != WeightUnit.KG
                            ) { Text("kg") }

                            OutlinedButton(
                                onClick = { profileViewModel.setWeightUnit(WeightUnit.LB) },
                                enabled = weightUnit != WeightUnit.LB
                            ) { Text("lb") }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text("Height", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { profileViewModel.setHeightUnit(HeightUnit.CM) },
                                enabled = heightUnit != HeightUnit.CM
                            ) { Text("cm") }

                            OutlinedButton(
                                onClick = { profileViewModel.setHeightUnit(HeightUnit.FT_IN) },
                                enabled = heightUnit != HeightUnit.FT_IN
                            ) { Text("ft-in") }
                        }
                    }
                }
            }

            Text(
                text = "Choose a profile",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))

            if (profiles.isEmpty()) {
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
                        val heightText = formatHeight(p.height, heightUnit)
                        val weightText = formatWeight(p.weight, weightUnit)

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

private fun formatWeight(kg: Int, unit: WeightUnit): String {
    if (kg == 0) return "Not set"
    return when (unit) {
        WeightUnit.KG -> "$kg kg"
        WeightUnit.LB -> {
            val lb = kg * 2.2046226218
            val text = String.format(Locale.US, "%.1f", lb)
                .removeSuffix(".0") // removes trailing .0 for whole numbers
            "$text lb"
        }
    }
}

private fun formatHeight(cm: Int, unit: HeightUnit): String {
    if (cm == 0) return "Not set"
    return when (unit) {
        HeightUnit.CM -> "$cm cm"
        HeightUnit.FT_IN -> {
            val totalInches = cm / 2.54
            var feet = floor(totalInches / 12.0).toInt()
            var inches = (totalInches - feet * 12.0).roundToInt()
            if (inches == 12) { feet += 1; inches = 0 }
            "$feet' $inches\""
        }
    }
}
