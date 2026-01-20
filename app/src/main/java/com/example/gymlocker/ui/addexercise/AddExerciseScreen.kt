package com.example.gymlocker.ui.addexercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.ui.theme.metalGloss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ExerciseFilterMode { BY_NAME, BY_MUSCLE_GROUP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercises) -> Unit
) {
    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val session = remember { SessionManager(context) }

    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)

    val allExercises by db.exerciseDao()
        .getAllExercises()
        .collectAsState(initial = emptyList())

    val allMuscleGroups by db.muscleGroupDao()
        .getAllMuscleGroups()
        .collectAsState(initial = emptyList())

    val muscleGroupNameById = remember(allMuscleGroups) {
        allMuscleGroups.associate { it.muscleGroupId to it.name }
    }

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(ExerciseFilterMode.BY_NAME) }

    // multi-select filter
    var selectedMuscleGroupIds by remember { mutableStateOf(setOf<Long>()) }

    // recent
    var recentExercises by remember { mutableStateOf<List<Exercises>>(emptyList()) }

    // Fetch recent once we have userId + exercises loaded
    LaunchedEffect(activeProfileUserId, allExercises) {
        val uid = activeProfileUserId
        if (uid == null || allExercises.isEmpty()) {
            recentExercises = emptyList()
            return@LaunchedEffect
        }

        recentExercises = withContext(Dispatchers.IO) {
            val ids = db.performedSetDao().getRecentExerciseIdsForUser(uid, limit = 5)
            ids.mapNotNull { id -> allExercises.firstOrNull { it.exerciseId == id } }
        }
    }

    val recentIds = remember(recentExercises) { recentExercises.map { it.exerciseId }.toSet() }

    val filtered = remember(allExercises, searchQuery, filterMode, selectedMuscleGroupIds, recentIds) {
        allExercises
            .filter { ex ->
                searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
            }
            .filter { ex ->
                if (filterMode == ExerciseFilterMode.BY_MUSCLE_GROUP) {
                    selectedMuscleGroupIds.isEmpty() || ex.muscleGroupId in selectedMuscleGroupIds
                } else true
            }
            // remove duplicates from the normal list
            .filterNot { it.exerciseId in recentIds }
    }

    val bottomEnabled = selectedExercises.isNotEmpty()
    val bottomText = when {
        selectedExercises.isEmpty() -> "Select exercises"
        selectedExercises.size == 1 -> "Add Exercise"
        else -> "Add Exercises"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    "Add Exercise",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Filter mode chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == ExerciseFilterMode.BY_NAME,
                        onClick = {
                            filterMode = ExerciseFilterMode.BY_NAME
                            selectedMuscleGroupIds = emptySet()
                        },
                        label = { Text("By name") }
                    )

                    FilterChip(
                        selected = filterMode == ExerciseFilterMode.BY_MUSCLE_GROUP,
                        onClick = { filterMode = ExerciseFilterMode.BY_MUSCLE_GROUP },
                        label = {
                            if (selectedMuscleGroupIds.isEmpty()) Text("By muscle group")
                            else Text("Muscles: ${selectedMuscleGroupIds.size}")
                        },
                        leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) }
                    )
                }

                if (filterMode == ExerciseFilterMode.BY_MUSCLE_GROUP) {
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { selectedMuscleGroupIds = emptySet() },
                                label = { Text("Clear") },
                                leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = null) }
                            )
                        }

                        items(allMuscleGroups) { mg ->
                            val selected = mg.muscleGroupId in selectedMuscleGroupIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedMuscleGroupIds =
                                        if (selected) selectedMuscleGroupIds - mg.muscleGroupId
                                        else selectedMuscleGroupIds + mg.muscleGroupId
                                },
                                label = {
                                    Text(
                                        mg.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for an exercise") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,

                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        errorTextColor = MaterialTheme.colorScheme.onSurface,

                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 96.dp)
                ) {
                    if (recentExercises.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        items(recentExercises, key = { it.exerciseId }) { exercise ->
                            val isSelected = selectedExercises.contains(exercise.exerciseId)

                            ExerciseListItem(
                                exercise = exercise,
                                selected = isSelected,
                                onClick = {
                                    selectedExercises = if (isSelected) {
                                        selectedExercises - exercise.exerciseId
                                    } else {
                                        selectedExercises + exercise.exerciseId
                                    }
                                },
                                muscleGroupNameById = muscleGroupNameById
                            )
                        }

                        item {
                            Spacer(Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    items(filtered, key = { it.exerciseId }) { exercise ->
                        val isSelected = selectedExercises.contains(exercise.exerciseId)

                        ExerciseListItem(
                            exercise = exercise,
                            selected = isSelected,
                            onClick = {
                                selectedExercises = if (isSelected) {
                                    selectedExercises - exercise.exerciseId
                                } else {
                                    selectedExercises + exercise.exerciseId
                                }
                            },
                            muscleGroupNameById = muscleGroupNameById
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        bottom = 32.dp,
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        val selected = allExercises.filter { it.exerciseId in selectedExercises }
                        selected.forEach { onExerciseSelected(it) }
                        onDismiss()
                    },
                    enabled = bottomEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(bottomText)
                }
            }
        }
    }
}

@Composable
fun ExerciseListItem(
    exercise: Exercises,
    selected: Boolean,
    onClick: () -> Unit,
    muscleGroupNameById: Map<Long, String>
) {
    val container =
        if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surface

    val content = MaterialTheme.colorScheme.onSurface
    val mgName = muscleGroupNameById[exercise.muscleGroupId] ?: "Other"
    val (icon, iconDesc) = iconForMuscleGroupName(mgName)

    val cardBorder = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .metalGloss(),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconDesc,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = exercise.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = content
            )
        }
    }
}


/**
 * Deterministic mapping via muscle group name.
 */
private fun iconForMuscleGroupName(name: String): Pair<ImageVector, String> {
    return when (name.trim().lowercase()) {
        "chest" -> Icons.Filled.Shield to "Chest"
        "legs" -> Icons.Filled.DirectionsWalk to "Legs"
        "back" -> Icons.Filled.Rowing to "Back"
        "shoulders" -> Icons.Filled.AccessibilityNew to "Shoulders"
        "arms" -> Icons.Filled.FitnessCenter to "Arms"
        else -> Icons.Filled.HelpOutline to name
    }
}
