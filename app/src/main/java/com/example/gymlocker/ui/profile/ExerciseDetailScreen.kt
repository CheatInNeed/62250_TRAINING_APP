package com.example.gymlocker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.PerformedSet
import com.example.gymlocker.data.entity.WeightUnit
import com.example.gymlocker.ui.settings.LocalUserSettings
import com.example.gymlocker.util.displayWeightFromKg
import com.example.gymlocker.util.formatWeight
import com.example.gymlocker.util.weightUnitLabel
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.ui.util.popBackUnlessAtRoot
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- NEW: segmented tabs (future-proof)
private enum class ExerciseDetailTab(val title: String) {
    OVERVIEW("Overview"),
    HISTORY("History")
}

// --- OLD: keep exactly the same data class fields/names as before (unchanged)
data class WorkoutSessionData(
    val workoutId: Long,
    val workoutName: String,
    val workoutDate: String,
    val sets: List<PerformedSet>,
    val totalVolume: Double,
    val maxWeight: Float,
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

    // NEW: weight unit (only used in new Overview tab; does NOT change old UI)
    val unit = LocalUserSettings.current.weightUnit

    // --- OLD state (unchanged)
    var exerciseName by remember { mutableStateOf("Loading...") }
    var muscleGroupName by remember { mutableStateOf("") }
    var personalRecord by remember { mutableStateOf("No data") }
    var totalSets by remember { mutableStateOf(0) }
    var totalVolume by remember { mutableStateOf(0.0) }
    var workoutSessions by remember { mutableStateOf<List<WorkoutSessionData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- NEW: dialog-like fields, but we DO NOT rewrite old UI; used only in Overview tab
    var lastTrainedText by remember { mutableStateOf("No data") }
    var prText by remember { mutableStateOf("No data") }

    // --- NEW: selected tab (default HISTORY so the screen "feels identical" when you open it)
    var selectedTab by rememberSaveable { mutableStateOf(ExerciseDetailTab.OVERVIEW) }

    val barShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(0.dp) }
    val cardShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(18.dp) }
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    LaunchedEffect(exerciseId, activeProfileUserId, unit) {
        if (activeProfileUserId == null) return@LaunchedEffect

        scope.launch {
            isLoading = true

            // --- OLD: Get exercise details (unchanged)
            val exercise = db.exerciseDao().getById(exerciseId)
            exerciseName = exercise?.name ?: "Unknown Exercise"
            muscleGroupName = db.muscleGroupDao().getNameById(exercise?.muscleGroupId ?: 0) ?: ""

            // --- OLD: Get PR (unchanged)
            val prSet = db.performedSetDao().getPersonalRecordSetForExerciseExcludingWorkout(
                exerciseId = exerciseId,
                excludeWorkoutId = null
            )
            personalRecord = if (prSet != null && prSet.reps > 0) {
                "${prSet.weight.toInt()} kg × ${prSet.reps} reps"
            } else if (prSet != null) {
                "${prSet.weight.toInt()} kg"
            } else {
                "No data"
            }

            // --- OLD: Get totals (unchanged)
            totalSets = db.performedSetDao().getTotalSetsForExercise(
                userId = activeProfileUserId!!,
                exerciseId = exerciseId
            )
            totalVolume = db.performedSetDao().getTotalVolumeForExercise(
                userId = activeProfileUserId!!,
                exerciseId = exerciseId
            )

            // --- OLD: Get workout history for this exercise (unchanged)
            val workoutIds = db.performedSetDao().getWorkoutIdsForExercise(
                userId = activeProfileUserId!!,
                exerciseId = exerciseId
            )

            val sessions = workoutIds.mapNotNull { workoutId ->
                val workout = db.workoutDao().getWorkoutById(workoutId) ?: return@mapNotNull null
                val sets = db.performedSetDao().getPerformedSetsForExerciseInWorkout(
                    workoutId = workoutId,
                    exerciseId = exerciseId
                ).filter { it.isCompleted }

                if (sets.isEmpty()) return@mapNotNull null

                val volume = sets.sumOf { (it.weight * it.reps).toDouble() }
                val maxWeight = sets.maxOfOrNull { it.weight } ?: 0f
                val totalReps = sets.sumOf { it.reps }

                WorkoutSessionData(
                    workoutId = workoutId,
                    workoutName = workout.name,
                    workoutDate = workout.date,
                    sets = sets,
                    totalVolume = volume,
                    maxWeight = maxWeight,
                    totalReps = totalReps
                )
            }

            workoutSessions = sessions

            // --- NEW (Overview only): last trained + PR text in current unit
            lastTrainedText = workoutSessions
                .maxByOrNull { parseDateOrNull(it.workoutDate) ?: LocalDateTime.MIN }
                ?.let { formatDate(it.workoutDate) }
                ?: "No data"

            // Keep old PR string for History, but Overview shows unit-aware if possible:
            prText = prSet?.let { set ->
                val shown = displayWeightFromKg(set.weight.toDouble(), unit)
                val unitLbl = weightUnitLabel(unit)
                if (set.reps > 0) "${formatWeight(shown, 0)} $unitLbl × ${set.reps} reps"
                else "${formatWeight(shown, 0)} $unitLbl"
            } ?: "No data"

            isLoading = false
        }
    }

    // --- OLD Scaffold kept exactly (unchanged)
    Scaffold(
        topBar = {
            val barShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)

            TopAppBar(
                modifier = Modifier.metalGloss(barShape),
                title = { Text(exerciseName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackUnlessAtRoot() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            val barShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)

            Surface(
                modifier = Modifier.metalGloss(barShape),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column {
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
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // --- NEW: segmented tabs inserted as first element (rest is untouched)
            item {
                ExerciseDetailSegmentedTabs(
                    selected = selectedTab,
                    onSelected = { selectedTab = it }
                )
            }

            when (selectedTab) {
                ExerciseDetailTab.OVERVIEW -> {
                    // --- NEW OVERVIEW TAB (added)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .metalGloss(cardShape),
                            shape = cardShape,
                            border = cardBorder,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Overview", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(12.dp))

                                KeyValueRow(label = "Muscle group", value = muscleGroupName)
                                Spacer(Modifier.height(6.dp))
                                KeyValueRow(label = "Last trained", value = lastTrainedText)
                                Spacer(Modifier.height(6.dp))
                                KeyValueRow(label = "Personal record", value = prText)
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .metalGloss(cardShape),
                            shape = cardShape,
                            border = cardBorder,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Heaviest weight per workout",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(12.dp))

                                val dayPoints = remember(workoutSessions, unit) {
                                    buildHeaviestPerDayLast3Months(workoutSessions, unit)
                                }

                                if (dayPoints.size < 2) {
                                    Text(
                                        text = "Not enough data to show a trend yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val axisColor = MaterialTheme.colorScheme.outlineVariant

                                    HeaviestWeightLineChartWithDates(
                                        points = dayPoints,
                                        unitLabel = weightUnitLabel(unit),
                                        lineColor = primaryColor,
                                        axisColor = axisColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Optional: keep the old "overall stats" info visible here too (but not required)
                    // If you want it: add another card, or reuse totalSets/totalVolume.
                }

                ExerciseDetailTab.HISTORY -> {
                    // ==========================
                    // ✅ OLD/GAMLE: 1:1 som før
                    // (Alt nedenfor er uændret kopi fra ExerciseDetailScreen_OLD.kt)
                    // ==========================

                    // Header Card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(exerciseName, style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = muscleGroupName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Stats Summary Card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Overall Statistics", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatColumn(label = "Personal Best", value = personalRecord)
                                    StatColumn(label = "Total Sets", value = totalSets.toString())
                                    StatColumn(
                                        label = "Total Volume",
                                        value = formatVolume(totalVolume)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "${workoutSessions.size} workouts performed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Workout History
                    item {
                        Text(
                            "Workout History",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (workoutSessions.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
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
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else {
                        items(workoutSessions, key = { it.workoutId }) { session ->
                            WorkoutSessionCard(session = session)
                        }
                    }
                }
            }
        }
    }
}

/* ==========================
   NEW: Segmented tabs (simple, future-proof)
   ========================== */
@Composable
private fun ExerciseDetailSegmentedTabs(
    selected: ExerciseDetailTab,
    onSelected: (ExerciseDetailTab) -> Unit
) {
    val cardShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(18.dp) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .metalGloss(cardShape),
        shape = cardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
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
            indicator = { /* no underline -> segmented feel */ },
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}

/* ==========================
   OLD: unchanged composables below
   ========================== */

@Composable
private fun WorkoutSessionCard(session: WorkoutSessionData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.workoutName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = formatDate(session.workoutDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
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

            // Header row for sets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET",
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "KG",
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "REPS",
                    modifier = Modifier.weight(1.0f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(4.dp))

            // Display sets
            session.sets.forEachIndexed { index, set ->
                if (set.isCompleted) {
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = set.weight.toInt().toString(),
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = set.reps.toString(),
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(label = "Volume", value = formatVolume(session.totalVolume))
                MiniStat(label = "Max Weight", value = "${session.maxWeight.toInt()} kg")
                MiniStat(label = "Total Reps", value = session.totalReps.toString())
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val output = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        LocalDateTime.parse(dateString, input).format(output)
    } catch (e: Exception) {
        dateString
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000 -> String.format(Locale.US, "%.1fM", volume / 1_000_000)
        volume >= 1_000 -> String.format(Locale.US, "%.1fK", volume / 1_000)
        else -> String.format(Locale.US, "%.0f", volume)
    }
}

/* ==========================
   NEW: Chart helpers (safe theme usage)
   ========================== */

private fun parseDateOrNull(raw: String): LocalDateTime? {
    return try {
        val input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        LocalDateTime.parse(raw, input)
    } catch (_: Exception) {
        null
    }
}

private data class DayPoint(
    val day: java.time.LocalDate,
    val weightShown: Double
)

private fun buildHeaviestPerDayLast3Months(
    sessions: List<WorkoutSessionData>,
    unit: WeightUnit
): List<DayPoint> {
    val cutoff = LocalDateTime.now().minusMonths(3)

    // 1) Filtrer til sidste 3 måneder
    val recent = sessions.mapNotNull { s ->
        val dt = parseDateOrNull(s.workoutDate) ?: return@mapNotNull null
        if (dt.isBefore(cutoff)) return@mapNotNull null
        val day = dt.toLocalDate()
        val shown = displayWeightFromKg(s.maxWeight.toDouble(), unit)
        day to shown
    }

    // 2) Group by day og tag max weight per day
    val byDayMax = recent
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, weights) -> weights.maxOrNull() ?: 0.0 }

    // 3) Sorter efter dato
    return byDayMax.entries
        .sortedBy { it.key }
        .map { DayPoint(day = it.key, weightShown = it.value) }
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
            color = MaterialTheme.colorScheme.outline
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

@Composable
private fun HeaviestWeightLineChartWithDates(
    points: List<DayPoint>,
    unitLabel: String,
    lineColor: Color,
    axisColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val weights = points.map { it.weightShown.toFloat() }
    val max = weights.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val min = weights.minOrNull() ?: 0f
    val range = (max - min).takeIf { it > 0f } ?: 1f

    val topLabel = "${formatWeight(max.toDouble(), decimals = 0)} $unitLabel"
    val dateFmt = remember { java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH) }

    // Labels: start, mid (optional), end
    val start = points.first().day
    val end = points.last().day
    val mid = if (points.size >= 3) points[points.size / 2].day else null

    Column(modifier = modifier) {
        Text(
            text = topLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        // Chart + x-axis labels
        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val w = size.width
                val h = size.height

                val padX = 14f
                val padY = 14f

                // X axis
                drawLine(
                    color = axisColor,
                    start = Offset(padX, h - padY),
                    end = Offset(w - padX, h - padY),
                    strokeWidth = 2f
                )

                val stepX = (w - 2f * padX) / (weights.size - 1).coerceAtLeast(1)

                fun yFor(v: Float): Float {
                    val t = (v - min) / range
                    return (h - padY) - t * (h - 2f * padY)
                }

                val path = Path()
                weights.forEachIndexed { i, v ->
                    val x = padX + i * stepX
                    val y = yFor(v)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                weights.forEachIndexed { i, v ->
                    val x = padX + i * stepX
                    val y = yFor(v)
                    drawCircle(
                        color = lineColor,
                        radius = 7f,
                        center = Offset(x, y)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // X-axis date labels row (start / mid / end)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = start.format(dateFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = mid?.format(dateFmt) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = end.format(dateFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

