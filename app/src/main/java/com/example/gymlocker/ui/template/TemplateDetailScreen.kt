package com.example.gymlocker.ui.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.template.TemplateExerciseWithSets
import com.example.gymlocker.data.entity.template.WorkoutTemplateWithExercises
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.metalGloss

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    navController: NavController,
    templateId: Long,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)

    val templateState = remember { mutableStateOf<WorkoutTemplateWithExercises?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    var reloadCounter by remember { mutableStateOf(0) }

    val pendingDeleteTemplateExerciseId = remember { mutableStateOf<Long?>(null) }
    val showDeleteConfirm = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // dialog for deleting whole template
    var showDeleteTemplateDialog by remember { mutableStateOf(false) }

    // Snackbar for feedback (e.g. deleted template)
    val snackbarHostState = remember { SnackbarHostState() }

    // starting workout when one is already active
    val isWorkoutInProgress by activeWorkoutViewModel.isWorkoutInProgress.collectAsState()
    var showDiscardToStartDialog by remember { mutableStateOf(false) }
    var pendingStartAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun reloadTemplate() {
        scope.launch {
            isLoading.value = true
            templateState.value = activeWorkoutViewModel.getTemplateWithExercises(templateId)
            isLoading.value = false
        }
    }

    LaunchedEffect(templateId, reloadCounter) {
        templateState.value = activeWorkoutViewModel.getTemplateWithExercises(templateId)
        isLoading.value = false
    }

    // Reload template whenever we come back to this screen
    LaunchedEffect(Unit) {
        val navBackStackEntry = navController.currentBackStackEntry
        val savedStateHandle = navBackStackEntry?.savedStateHandle ?: return@LaunchedEffect

        // Check if we should reload (set by EditTemplateScreen on save)
        val shouldReload = savedStateHandle.get<Boolean>("shouldReloadTemplate") ?: false
        if (shouldReload) {
            reloadCounter++
            savedStateHandle.set("shouldReloadTemplate", false)
        }
    }

    if (showDeleteConfirm.value && pendingDeleteTemplateExerciseId.value != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm.value = false
                pendingDeleteTemplateExerciseId.value = null
            },
            title = { Text("Remove exercise?") },
            text = { Text("Are you sure you want to remove this exercise from the template?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = pendingDeleteTemplateExerciseId.value!!
                    showDeleteConfirm.value = false
                    pendingDeleteTemplateExerciseId.value = null

                    scope.launch {
                        activeWorkoutViewModel.deleteTemplateExerciseById(id)
                        reloadTemplate()
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm.value = false
                    pendingDeleteTemplateExerciseId.value = null
                }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    // Existing: delete exercise from template ...

    if (showDeleteTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTemplateDialog = false },
            title = { Text("Delete template?") },
            text = {
                Text("This will delete the template and all its exercises/sets. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteTemplateDialog = false
                        scope.launch {
                            activeWorkoutViewModel.deleteTemplate(templateId)
                            // Show feedback
                            snackbarHostState.showSnackbar("Template deleted")
                            // Go back after deletion so user doesn't stay on a 'missing' screen
                            navController.popBackUnlessAtRoot()
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTemplateDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }


    if (showDiscardToStartDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardToStartDialog = false
                pendingStartAction = null
            },
            title = { Text("Discard current workout?") },
            text = { Text("You have an active workout. Discard it and start the new one?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Reset sker med det samme, så vi kan starte ny bagefter
                        activeWorkoutViewModel.discardWorkout()

                        val action = pendingStartAction
                        showDiscardToStartDialog = false
                        pendingStartAction = null

                        action?.invoke()
                    }
                ) { Text("Discard & start") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardToStartDialog = false
                        pendingStartAction = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    val template = templateState.value

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = { Text("Template") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.metalGloss(BotBarShape),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column {
                    ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                    AppBottomBar(navController)
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (template != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = template.template.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = template.template.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        enabled = activeProfileUserId != null,
                        onClick = {
                            val profileId = activeProfileUserId ?: return@IconButton

                            val dateString =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                    .format(Date())

                            // Det vi vil gøre, hvis vi må starte
                            val startTemplate: () -> Unit = {
                                scope.launch {
                                    activeWorkoutViewModel.startWorkoutFromTemplate(
                                        templateId = templateId,
                                        userId = profileId,
                                        date = dateString
                                    )

                                    navController.navigate("activeWorkout") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            if (isWorkoutInProgress) {
                                pendingStartAction = startTemplate
                                showDiscardToStartDialog = true
                                return@IconButton
                            }

                            startTemplate()
                        }
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Start workout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { navController.navigate("editTemplate/$templateId") }
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit template",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                activeWorkoutViewModel.toggleTemplateFavorite(templateId)
                                reloadTemplate()
                            }
                        }
                    ) {
                        Icon(
                            if (template.template.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Toggle favorite",
                            tint = if (template.template.isFavorite)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            showDeleteTemplateDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete template",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(template.exercises) { exerciseWithSets ->
                        ExerciseCard(
                            exerciseWithSets = exerciseWithSets,
                            onRequestDelete = {
                                pendingDeleteTemplateExerciseId.value = exerciseWithSets.templateExercise.id
                                showDeleteConfirm.value = true
                            },
                            onOpenExercise = { exerciseId ->
                                navController.navigate("exerciseDetail/$exerciseId")
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Template not found",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exerciseWithSets: TemplateExerciseWithSets,
    onRequestDelete: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    exerciseName: String = "Unknown Exercise"
) {
    val settings = LocalUserSettings.current
    val unit = settings.weightUnit
    val context = LocalContext.current
    val fetchedExerciseName = remember { mutableStateOf(exerciseName) }

    LaunchedEffect(exerciseWithSets.templateExercise.exerciseId) {
        val db = AppDatabase.getDatabase(context)
        val exercise = db.exerciseDao().getById(exerciseWithSets.templateExercise.exerciseId)
        fetchedExerciseName.value = exercise?.name ?: "Unknown Exercise"
    }

    val cardShape = RoundedCornerShape(18.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .metalGloss(cardShape),
        shape = cardShape,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fetchedExerciseName.value,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onOpenExercise(exerciseWithSets.templateExercise.exerciseId)
                        },
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onRequestDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete exercise",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            exerciseWithSets.sets.forEach { set ->
                val shown = displayWeightFromKg(set.weight.toDouble(), unit)
                Text(
                    text = "Set ${set.setNumber}: ${formatWeight(shown, decimals = 0)} ${weightUnitLabel(unit)} × ${set.reps} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
