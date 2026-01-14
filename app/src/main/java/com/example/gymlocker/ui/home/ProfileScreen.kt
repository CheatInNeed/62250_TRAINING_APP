package com.example.gymlocker.ui.profile

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.AuthViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

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

    // ✅ NEW: Active profile photo uri (stored in SessionManager/DataStore)
    val photoUriString by profileViewModel.activeProfilePhotoUri.collectAsState()

    val context = LocalContext.current

    // Dialog / UI state
    var deleteTargetUserId by remember { mutableStateOf<Long?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var showPhotoMenu by remember { mutableStateOf(false) }
    var showPermissionDenied by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    // Permission: used to satisfy the "deny permission shows explanation" AC
    val permission = remember {
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // Persist URI read access across restarts
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Some providers/devices may not allow persistable permission.
            // We'll still store it; if it doesn't persist, UI falls back to default avatar.
        }

        profileViewModel.setActiveProfilePhoto(uri.toString())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pickPhotoLauncher.launch(arrayOf("image/*"))
        } else {
            showPermissionDenied = true
        }
    }

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

    // Error dialog
    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Oops") },
            text = { Text(errorMsg ?: "") },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("OK") } }
        )
    }

    // Permission denied explanation (AC)
    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            title = { Text("Photo permission denied") },
            text = {
                Text(
                    "You can keep using the app without a profile photo. " +
                            "If you want to add one later, allow photo access in system settings."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDenied = false }) { Text("OK") }
            }
        )
    }

    // Photo menu (choose/remove)
    if (showPhotoMenu) {
        AlertDialog(
            onDismissRequest = { showPhotoMenu = false },
            title = { Text("Profile photo") },
            text = { Text("Choose a photo or remove the current one.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPhotoMenu = false
                        permissionLauncher.launch(permission)
                    }
                ) { Text("Choose photo") }
            },
            dismissButton = {
                Row {
                    if (!photoUriString.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                showPhotoMenu = false
                                showRemoveConfirm = true
                            }
                        ) { Text("Remove") }
                    }
                    TextButton(onClick = { showPhotoMenu = false }) { Text("Cancel") }
                }
            }
        )
    }

    // Remove confirm
    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove photo?") },
            text = { Text("This will restore the default avatar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirm = false
                        profileViewModel.removeActiveProfilePhoto()
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
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
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
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

            // Active profile card
            activeProfile?.let { p ->
                val heightText = if (p.height == 0) "Not set" else "${p.height} cm"

                val settings = LocalUserSettings.current
                val unit = settings.weightUnit

                val weightText =
                    if (p.weight == 0) "Not set"
                    else {
                        val shown = displayWeightFromKg(p.weight.toDouble(), unit)
                        "${formatWeight(shown, decimals = 0)} ${weightUnitLabel(unit)}"
                    }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(
                                uriString = photoUriString,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { showPhotoMenu = true }
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active profile", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    p.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(onClick = { showPhotoMenu = true }) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Change photo")
                            }
                        }

                        Spacer(Modifier.height(10.dp))

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

@Composable
private fun ProfileAvatar(
    uriString: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageBitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, uriString) {
        value = null
        if (uriString.isNullOrBlank()) return@produceState

        value = withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default avatar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
