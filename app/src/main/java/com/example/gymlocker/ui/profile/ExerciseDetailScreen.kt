package com.example.gymlocker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.PerformedSet
import com.example.gymlocker.data.entity.WeightUnit
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ExerciseDetailTab(val title: String) {
    OVERVIEW("Overview"),
    HISTORY("History")
    // Future:
    // PRS("PRs"),
}

data class WorkoutSessionData(
    val workoutId: Long,
    val workoutName: String,
    val workoutDate: String,
    val sets: List<PerformedSet>,
    val totalVolumeKg: Double,   // stored in kg*reps
    val maxWeightKg: Float,      // stored in kg
    val totalReps: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val activeProfileUserId by profileViewModel.activeProfileUserId.collectAsState()
    val unit = LocalUserSettings.current.weightUnit

    // Peter Standard: bar shape = 0dp corners
    val barShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(0.dp) }

    // Cards: share shape between Card + metalGloss
    val cardShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(18.dp) }
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    var isLoading by remember { mutableStateOf(true) }

    // Core fields (old + new)
    var exerciseName by remember { mutableStateOf("Loading…") }
    var muscleGroupName by remember { mutableStateOf("—") }

    // New (dialog-like fields)
    var prText by remember { mutableStateOf("—") }
    var lastTrainedText by remember { mutableStateOf("—") }

    // Old totals
    var totalSets by remember { mutableStateOf(0) }
    var totalVolumeKg by remember { mutableStateOf(0.0) }

    // Old history
    var workoutSessions by remember { mutableStateOf<List<WorkoutSessionData>>(emptyList()) }

    // Tabs
    var selectedTab by rememberSaveable { mutableStateOf(ExerciseDetailTab.OVERVIEW) }

    LaunchedEffect(exerciseId, activeProfileUserId, unit) {
        val userId = activeProfileUserId ?: return@LaunchedEffect
        isLoading = true

        scope.launch {
            // Exercise core
            val exercise = db.exerciseDao().getById(exerciseId)
            exerciseName = exercise?.name ?: "Unknown exercise"
            muscleGroupName = db.muscleGroupDao().getNameById(exercise?.muscleGroupId ?: 0) ?: "—"

            // Dialog-like stats via VM (respects unit + your existing formatting)
            val statsUi = activeWorkoutViewModel.getExerciseStatsUi(exerciseId, unit)
            prText = statsUi.prText
            lastTrainedText = statsUi.lastTrainedText

            // Old totals (DB)
            totalSets = db.performedSetDao().getTotalSetsForExercise(userId = userId, exerciseId = exerciseId)
            totalVolumeKg = db.performedSetDao().getTotalVolumeForExercise(userId = userId, exerciseId = exerciseId)

            // Old workout history
            val workoutIds = db.performedSetDao().getWorkoutIdsForExercise(userId = userId, exerciseId = exerciseId)

            val sessions = workoutIds.mapNotNull { workoutId ->
                val workout = db.workoutDao().getWorkoutById(workoutId) ?: return@mapNotNull null
                val sets = db.performedSetDao().getPerformedSetsForExerciseInWorkout(
                    workoutId = workoutId,
                    exerciseId = exerciseId
                ).filter { it.isCompleted }

                if (sets.isEmpty()) return@mapNotNull null

                val volumeKg = sets.sumOf { (it.weight * it.reps).toDouble() }
                val maxKg = sets.maxOfOrNull { it.weight } ?: 0f
                val reps = sets.sumOf { it.reps }

                WorkoutSessionData(
                    workoutId = workoutId,
                    workoutName = workout.name,
                    workoutDate = workout.date,
                    sets = sets,
                    totalVolumeKg = volumeKg,
                    maxWeightKg = maxKg,
                    totalReps = reps
                )
            }.sortedBy { parseDateOrNull(it.workoutDate) ?: LocalDateTime.MIN }

            workoutSessions = sessions
            isLoading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(barShape),
                title = {
                    Text(
                        text = exerciseName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.metalGloss(barShape),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column {
                    ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                    AppBottomBar(navController)
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Segmented controls
            item {
                ExerciseDetailSegmentedTabs(
                    selected = selectedTab,
                    onSelected = { selectedTab = it },
                    cardShape = cardShape,
                    cardBorder = cardBorder
                )
            }

            when (selectedTab) {
                ExerciseDetailTab.OVERVIEW -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().metalGloss(cardShape),
                            shape = cardShape,
                            border = cardBorder,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Overview", style = MaterialTheme.typography.titleMedium)

                                KeyValueRow("Muscle group", muscleGroupName)
                                KeyValueRow("Last trained", lastTrainedText)
                                KeyValueRow("Personal record", prText)

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Old "overall stats" kept (but nicer)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatChip(label = "Total sets", value = totalSets.toString())
                                    StatChip(
                                        label = "Total volume",
                                        value = formatVolumeShown(totalVolumeKg, unit)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        val points3m = remember(workoutSessions, unit) {
                            buildHeaviestSeriesLast3Months(workoutSessions, unit)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().metalGloss(cardShape),
                            shape = cardShape,
                            border = cardBorder,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Heaviest weight per workout",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Last 3 months",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (points3m.size < 2) {
                                    Text(
                                        text = "Not enough data to show a trend yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    // ✅ Important: read theme colors OUTSIDE Canvas
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val axisColor = MaterialTheme.colorScheme.outlineVariant

                                    HeaviestWeightLineChart(
                                        points = points3m.map { it.weightShown.toFloat() },
                                        unitLabel = weightUnitLabel(unit),
                                        lineColor = primaryColor,
                                        axisColor = axisColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                ExerciseDetailTab.HISTORY -> {
                    // Keep old header cards + workout history list
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().metalGloss(cardShape),
                            shape = cardShape,
                            border = cardBorder
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(exerciseName, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = muscleGroupName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "${workoutSessions.size} workouts performed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Workout History",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (workoutSessions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().metalGloss(cardShape),
                                shape = cardShape,
                                border = cardBorder
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No workout history for this exercise",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(workoutSessions, key = { it.workoutId }) { session ->
                            WorkoutSessionCard(
                                session = session,
                                unit = unit,
                                cardShape = cardShape,
                                cardBorder = cardBorder
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailSegmentedTabs(
    selected: ExerciseDetailTab,
    onSelected: (ExerciseDetailTab) -> Unit,
    cardShape: androidx.compose.ui.graphics.Shape,
    cardBorder: BorderStroke
) {
    Card(
        modifier = Modifier.fillMaxWidth().metalGloss(cardShape),
        shape = cardShape,
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        val tabs = remember { ExerciseDetailTab.entries }
        TabRow(
            selectedTabIndex = tabs.indexOf(selected),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { /* no underline */ },
            divider = { /* no divider */ }
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                Tab(
                    selected = isSelected,
                    onClick = { onSelected(tab) },
                    text = {
                        Text(
                            text = tab.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/* -------------------- History card (old UI kept, but unit-aware) -------------------- */

@Composable
private fun WorkoutSessionCard(
    session: WorkoutSessionData,
    unit: WeightUnit,
    cardShape: androidx.compose.ui.graphics.Shape,
    cardBorder: BorderStroke
) {
    val unitLabel = weightUnitLabel(unit)

    Card(
        modifier = Modifier.fillMaxWidth().metalGloss(cardShape),
        shape = cardShape,
        border = cardBorder
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(session.workoutName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = formatDate(session.workoutDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${session.sets.size} sets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Header row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SET",
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = unitLabel,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "REPS",
                    modifier = Modifier.weight(1.0f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            session.sets.forEachIndexed { index, set ->
                if (!set.isCompleted) return@forEachIndexed

                val shown = displayWeightFromKg(set.weight.toDouble(), unit)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set ${index + 1}",
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatWeight(shown, decimals = 0),
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = set.reps.toString(),
                        modifier = Modifier.weight(1.0f),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Session: ${formatWeight(displayWeightFromKg(session.maxWeightKg.toDouble(), unit), 0)} $unitLabel max · " +
                        "${session.totalReps} reps · ${formatVolumeShown(session.totalVolumeKg, unit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* -------------------- Overview chart (new) -------------------- */

private data class HeaviestPoint(val date: LocalDateTime, val weightShown: Double)

private fun buildHeaviestSeriesLast3Months(
    sessions: List<WorkoutSessionData>,
    unit: WeightUnit
): List<HeaviestPoint> {
    val cutoff = LocalDateTime.now().minusMonths(3)
    return sessions.mapNotNull { s ->
        val dt = parseDateOrNull(s.workoutDate) ?: return@mapNotNull null
        if (dt.isBefore(cutoff)) return@mapNotNull null
        val shown = displayWeightFromKg(s.maxWeightKg.toDouble(), unit)
        HeaviestPoint(dt, shown)
    }.sortedBy { it.date }
}

@Composable
private fun HeaviestWeightLineChart(
    points: List<Float>,
    unitLabel: String,
    lineColor: Color,
    axisColor: Color,
    modifier: Modifier = Modifier
) {
    val max = points.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val min = points.minOrNull() ?: 0f
    val range = (max - min).takeIf { it > 0f } ?: 1f

    val topLabel = "${formatWeight(max.toDouble(), decimals = 0)} $unitLabel"

    Column(modifier = modifier) {
        Text(
            text = topLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val padX = 10f
            val padY = 10f

            // Axis
            drawLine(
                color = axisColor,
                start = Offset(padX, h - padY),
                end = Offset(w - padX, h - padY),
                strokeWidth = 2f
            )

            val stepX = (w - 2f * padX) / (points.size - 1).coerceAtLeast(1)

            fun yFor(v: Float): Float {
                val t = (v - min) / range
                return (h - padY) - t * (h - 2f * padY)
            }

            val path = Path()
            points.forEachIndexed { i, v ->
                val x = padX + i * stepX
                val y = yFor(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            points.forEachIndexed { i, v ->
                val x = padX + i * stepX
                val y = yFor(v)
                drawCircle(
                    color = lineColor,
                    radius = 7f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/* -------------------- Helpers (old file had these; kept here) -------------------- */

private fun parseDateOrNull(raw: String): LocalDateTime? {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        LocalDateTime.parse(raw, input)
    } catch (_: Exception) {
        null
    }
}

private fun formatDate(raw: String): String {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        LocalDateTime.parse(raw, input).format(output)
    } catch (e: Exception) {
        raw
    }
}

/**
 * Total volume stored as "kg * reps". We show it in current unit.
 * If you want true "lb volume", we convert kg->lb consistently here.
 */
private fun formatVolumeShown(volumeKg: Double, unit: WeightUnit): String {
    val shown = when (unit) {
        WeightUnit.KG -> volumeKg
        WeightUnit.LB -> volumeKg * 2.2046226218
    }
    return when {
        shown >= 1_000_000 -> String.format(Locale.US, "%.1fM", shown / 1_000_000)
        shown >= 1_000 -> String.format(Locale.US, "%.1fK", shown / 1_000)
        else -> String.format(Locale.US, "%.0f", shown)
    } + " " + (if (unit == WeightUnit.KG) "kg" else "lb")
}
