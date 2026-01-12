package com.example.gymlocker.ui.template

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val templateState = remember { mutableStateOf<com.example.gymlocker.data.entity.template.WorkoutTemplateWithExercises?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    var reloadCounter by remember { mutableStateOf(0) }

    val pendingDeleteTemplateExerciseId = remember { mutableStateOf<Long?>(null) }
    val showDeleteConfirm = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(templateId) { reloadTemplate() }

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
                Text("Loading.")
            }
            return@Scaffold
        }

        if (template == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Template not found.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = template.template.name, style = MaterialTheme.typography.headlineSmall)
                    Text(text = template.template.date, style = MaterialTheme.typography.bodySmall)
                }

                IconButton(
                    enabled = activeProfileUserId != null,
                    onClick = {
                        val profileId = activeProfileUserId ?: return@IconButton

                        val dateString =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                        activeWorkoutViewModel.startWorkoutFromTemplate(
                            templateId = templateId,
                            userId = profileId,
                            date = dateString
                        )

                        navController.navigate("activeWorkout") { launchSingleTop = true }
                    }
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start workout")
                }
            }
                            navController.navigate("activeWorkout") {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start workout")
                    }
                    IconButton(
                        onClick = {
                            navController.navigate("editTemplate/$templateId")
                        }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit template")
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
                            tint = if (template.template.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Exercises", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(template.exercises) { exerciseWithSets ->
                    Text("• ExerciseId: ${exerciseWithSets.templateExercise.exerciseId} (${exerciseWithSets.sets.size} sets)")
                }
            }
        }
    }
}
