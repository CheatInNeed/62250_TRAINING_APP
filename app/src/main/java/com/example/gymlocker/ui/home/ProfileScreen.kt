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
import androidx.compose.material3.FilterChip
import com.example.gymlocker.ui.components.MuscleGroupDistributionChart
import com.example.gymlocker.ui.components.PeriodBarChart
import com.example.gymlocker.data.dao.MuscleGroupDistributionRow
import com.example.gymlocker.viewmodel.WeekHoursUi
import com.example.gymlocker.viewmodel.WeekVolumeUi
import com.example.gymlocker.viewmodel.MonthHoursUi
import com.example.gymlocker.viewmodel.MonthVolumeUi
import com.example.gymlocker.viewmodel.StatsRange
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlocker.viewmodel.StatViewModel
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.gymlocker.ui.components.ProfileAvatarIcon
import com.example.gymlocker.data.auth.SessionManager

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
    val statViewModel: StatViewModel = viewModel()

    // Active profile photo uri (stored in SessionManager/DataStore)
    val photoUriString by profileViewModel.activeProfilePhotoUri.collectAsState()

    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }

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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Delete profile?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will delete \"$deleteTargetName\" and all workouts/templates linked to it.\n\nThis cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetUserId = null }) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    // Error dialog
    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Oops",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { errorMsg = null }) {
                    Text(
                        text = "OK",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    // Permission denied explanation (AC)
    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Photo permission denied",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "You can keep using the app without a profile photo. If you want to add one later, allow photo access in system settings.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDenied = false }) {
                    Text(
                        text = "OK",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    // Photo menu (choose/remove)
    if (showPhotoMenu) {
        AlertDialog(
            onDismissRequest = { showPhotoMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Profile photo",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Choose a photo or remove the current one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPhotoMenu = false
                        permissionLauncher.launch(permission)
                    }
                ) {
                    Text(
                        text = "Choose photo",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                Row {
                    if (!photoUriString.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                showPhotoMenu = false
                                showRemoveConfirm = true
                            }
                        ) {
                            Text(
                                text = "Remove",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TextButton(onClick = { showPhotoMenu = false }) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }

    // Remove confirm
    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Remove photo?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will restore the default avatar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirm = false
                        profileViewModel.removeActiveProfilePhoto()
                    }
                ) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    var isProfilePickerOpen by remember { mutableStateOf(false) }
    var isAvatarMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { if (profiles.isNotEmpty()) isProfilePickerOpen = true }
                        ) {
                            Text(
                                text = activeProfile?.name ?: "Select profile",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Icon(
                                imageVector = if (isProfilePickerOpen) {
                                    Icons.Filled.ArrowDropUp
                                } else {
                                    Icons.Filled.ArrowDropDown
                                },
                                contentDescription = "Select profile",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isProfilePickerOpen,
                            onDismissRequest = { isProfilePickerOpen = false },
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .widthIn(min = 180.dp)
                        ) {
                            profiles.forEachIndexed { index, profile ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    leadingIcon = {
                                        val uriForThisProfile by session
                                            .profilePhotoUri(profile.userId)
                                            .collectAsState(initial = null)

                                        val isActive = profile.userId == activeProfileUserId
                                        ProfileAvatarIcon(
                                            uriString = uriForThisProfile,
                                            size = 24.dp,
                                            modifier = Modifier
                                                .background(
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = CircleShape
                                                )
                                                .padding(2.dp)
                                        )
                                    },

                                    onClick = {
                                        profileViewModel.setActiveProfile(profile.userId)
                                        isProfilePickerOpen = false
                                    }
                                )

                                // subtle divider between items (except last)
                                if (index < profiles.lastIndex) {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Single burger menu (top-right)
                    Box {
                        IconButton(onClick = { isAvatarMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open menu"
                            )
                        }

                        DropdownMenu(
                            expanded = isAvatarMenuOpen,
                            onDismissRequest = { isAvatarMenuOpen = false },
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .widthIn(min = 200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit profile") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isAvatarMenuOpen = false
                                    navController.navigate("editProfile")
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("View statistics") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.BarChart,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isAvatarMenuOpen = false
                                    navController.navigate("profileStats")
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isAvatarMenuOpen = false
                                    navController.navigate("settings")
                                }
                            )

                            Divider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Log out",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    isAvatarMenuOpen = false
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
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
                .verticalScroll(rememberScrollState())
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
                        .padding(bottom = 14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
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
                                Text(
                                    text = "Active profile",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(onClick = { showPhotoMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Change photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Height: $heightText  |  Weight: $weightText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (profiles.isEmpty()) {
                NoProfilesCard(
                    onCreateProfileClick = { navController.navigate("createProfile") }
                )
            } else if (activeProfileUserId != null) {
                val userId = activeProfileUserId!!

                // Same data sources as HomeScreen
                val statsRange by statViewModel.statsRange.collectAsState()

                val weeklyHours by statViewModel
                    .weeklyHoursLast3Months(userId)
                    .collectAsState(initial = emptyList())

                val weeklyVolume by statViewModel
                    .weeklyVolumeLast3Months(userId)
                    .collectAsState(initial = emptyList())

                val distribution by statViewModel
                    .muscleGroupDistribution(userId)
                    .collectAsState(initial = emptyList())

                val monthlyHours by statViewModel
                    .monthlyHoursLast12Months(userId)
                    .collectAsState(initial = emptyList())

                val monthlyVolume by statViewModel
                    .monthlyVolumeLast12Months(userId)
                    .collectAsState(initial = emptyList())

                // Make the whole card clickable -> dedicated statistics page
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("profileStats") }
                ) {
                    StatsCard(
                        weeklyHours = weeklyHours,
                        weeklyVolume = weeklyVolume,
                        monthlyHours = monthlyHours,
                        monthlyVolume = monthlyVolume,
                        distribution = distribution,
                        statsRange = statsRange,
                        onRangeChange = { statViewModel.setStatsRange(it) }
                    )
                }
            } else {
                Text(
                    text = "Select a profile to see stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
private fun NoProfilesCard(
    onCreateProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No profiles yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Create a profile to start tracking your workouts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onCreateProfileClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create Profile")
            }
        }
    }
}
private enum class GraphMode { HOURS, VOLUME }

@Composable
fun StatsCard(
    weeklyHours: List<WeekHoursUi>,
    weeklyVolume: List<WeekVolumeUi>,
    monthlyHours: List<MonthHoursUi>,
    monthlyVolume: List<MonthVolumeUi>,
    distribution: List<MuscleGroupDistributionRow>,
    statsRange: StatsRange,
    onRangeChange: (StatsRange) -> Unit
) {
    var mode by remember { mutableStateOf(GraphMode.HOURS) }

    // --- NEW: limit months to last 6 for the compact card ---
    val last6MonthlyHours = monthlyHours.takeLast(6)
    val last6MonthlyVolume = monthlyVolume.takeLast(6)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Stats",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            // Week/Month + Hours/Volume toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = statsRange == StatsRange.WEEK,
                        onClick = { onRangeChange(StatsRange.WEEK) },
                        label = { Text("Week") }
                    )
                    FilterChip(
                        selected = statsRange == StatsRange.MONTH,
                        onClick = { onRangeChange(StatsRange.MONTH) },
                        label = { Text("Month") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == GraphMode.HOURS,
                        onClick = { mode = GraphMode.HOURS },
                        label = { Text("Hours") }
                    )
                    FilterChip(
                        selected = mode == GraphMode.VOLUME,
                        onClick = { mode = GraphMode.VOLUME },
                        label = { Text("Volume") }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = when (statsRange) {
                    StatsRange.WEEK ->
                        if (mode == GraphMode.HOURS)
                            "Hours trained per week (last 3 months)"
                        else
                            "Volume per week (last 3 months)"
                    StatsRange.MONTH ->
                        if (mode == GraphMode.HOURS)
                            "Hours trained per month (last 6 months)"
                        else
                            "Volume per month (last 6 months)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(10.dp))

            // --------- CHART AREA (PeriodBarChart) ---------
            when (statsRange) {
                StatsRange.WEEK -> {
                    val weekLabels = weeklyHours.map {
                        it.weekStart.get(WeekFields.ISO.weekOfWeekBasedYear()).toString()
                    }

                    if (mode == GraphMode.HOURS) {
                        PeriodBarChart(
                            values = weeklyHours.map { it.hours },
                            labels = weekLabels,
                            xCaption = "Week",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        PeriodBarChart(
                            values = weeklyVolume.map { it.volume },
                            labels = weekLabels,
                            xCaption = "Week",
                            yTickStep = 500f, // 500 kg per tick
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                StatsRange.MONTH -> {
                    if (mode == GraphMode.HOURS) {
                        val monthLabels = last6MonthlyHours.map {
                            it.yearMonth.month.getDisplayName(
                                TextStyle.SHORT,
                                Locale.getDefault()
                            )
                        }

                        PeriodBarChart(
                            values = last6MonthlyHours.map { it.hours },
                            labels = monthLabels,
                            xCaption = "Month",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val monthLabels = last6MonthlyVolume.map {
                            it.yearMonth.month.getDisplayName(
                                TextStyle.SHORT,
                                Locale.getDefault()
                            )
                        }

                        PeriodBarChart(
                            values = last6MonthlyVolume.map { it.volume },
                            labels = monthLabels,
                            xCaption = "Month",
                            yTickStep = 500f, // 500 kg per tick
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (statsRange == StatsRange.WEEK)
                    "Training balance (this week)"
                else
                    "Training balance (this month)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(10.dp))

            MuscleGroupDistributionChart(
                rows = distribution,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


