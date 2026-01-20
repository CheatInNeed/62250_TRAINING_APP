package com.example.gymlocker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.database.AppDatabase
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
    OVERVIEW("Overview")
    // Future tabs:
    // HISTORY("History"),
    // PRS("PRs"),
}

data class WorkoutSessionData(
    val workoutId: Long,
    val workoutName: String,
    val workoutDate: String, // "yyyy-MM-dd HH:mm:ss.SSS"
    val maxWeightKg: Float
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

    // Peter Standard: cards must share shape between Card + metalGloss
    val cardShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(18.dp) }
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))

    var isLoading by remember { mutableStateOf(true) }

    var exerciseName by remember { mutableStateOf("Loading…") }
    var muscleGroupName by remember { mutableStateOf("—") }
    var prText by remember { mutableStateOf("—") }
    var lastTrainedText by remember { mutableStateOf("—") }

    // For graph (3 months)
    var sessions by remember { mutableStateOf<List<WorkoutSessionData>>(emptyList()) }

    // Tabs
    var selectedTab by rememberSaveable { mutableStateOf(ExerciseDetailTab.OVERVIEW) }

    LaunchedEffect(exerciseId, activeProfileUserId, unit) {
        val userId = activeProfileUserId ?: return@LaunchedEffect
        isLoading = true

        scope.launch {
            // Exercise core
            val exercise = db.exerciseDao().getById(exerciseId)
            exerciseName = exercise?.name ?: "Unknown exercise"
            muscleGroupName =
                db.muscleGroupDao().getNameById(exercise?.muscleGroupId ?: 0) ?: "—"

            // Reuse existing VM formatting for PR + Last trained (respects unit)
            val stats = activeWorkoutViewModel.getExerciseStatsUi(exerciseId, unit)
            prText = stats.prText
            lastTrainedText = stats.lastTrainedText

            // Build sessions (max weight per workout)
            val workoutIds = db.performedSetDao().getWorkoutIdsForExercise(
                userId = userId,
                exerciseId = exerciseId
            )

            val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

            val built = workoutIds.mapNotNull { workoutId ->
                val workout = db.workoutDao().getWorkoutById(workoutId) ?: return@mapNotNull null

                val sets = db.performedSetDao().getPerformedSetsForExerciseInWorkout(
                    workoutId = workoutId,
                    exerciseId = exerciseId
                ).filter { it.isCompleted }

                val maxKg = sets.maxOfOrNull { it.weight } ?: return@mapNotNull null

                WorkoutSessionData(
                    workoutId = workoutId,
                    workoutName = workout.name,
                    workoutDate = workout.date,
                    maxWeightKg = maxKg
                )
            }
                .sortedBy { runCatching { LocalDateTime.parse(it.workoutDate, dateFmt) }.getOrNull() }

            sessions = built
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Segmented controls (tabs) – built to scale to more tabs later
            item {
                ExerciseDetailSegmentedTabs(
                    selected = selectedTab,
                    onSelected = { selectedTab = it },
                    cardShape = cardShape,
                    cardBorder = cardBorder
                )
            }

            // Overview tab (only tab for now)
            if (selectedTab == ExerciseDetailTab.OVERVIEW) {
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
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.titleMedium
                            )

                            // Match real-world + recognition: show the dialog fields immediately
                            KeyValueRow(label = "Muscle group", value = muscleGroupName)
                            KeyValueRow(label = "Last trained", value = lastTrainedText)
                            KeyValueRow(label = "Personal record", value = prText)
                        }
                    }
                }

                item {
                    val points3m = remember(sessions, unit) {
                        buildHeaviestSeriesLast3Months(sessions, unit)
                    }

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
    // “Segmented” feel: a single card holding the tabs, clear selected state.
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
        val tabs = remember { ExerciseDetailTab.entries }

        TabRow(
            selectedTabIndex = tabs.indexOf(selected),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { /* no underline -> more "segmented" */ },
            divider = { /* avoid extra divider noise */ }
        ) {
            tabs.forEachIndexed { index, tab ->
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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

private data class HeaviestPoint(val date: LocalDateTime, val weightShown: Double)

private fun buildHeaviestSeriesLast3Months(
    sessions: List<WorkoutSessionData>,
    unit: com.example.gymlocker.data.entity.WeightUnit
): List<HeaviestPoint> {
    val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    val cutoff = LocalDateTime.now().minusMonths(3)

    return sessions.mapNotNull { s ->
        val dt = runCatching { LocalDateTime.parse(s.workoutDate, dateFmt) }.getOrNull() ?: return@mapNotNull null
        if (dt.isBefore(cutoff)) return@mapNotNull null
        val shown = displayWeightFromKg(s.maxWeightKg.toDouble(), unit)
        HeaviestPoint(dt, shown)
    }.sortedBy { it.date }
}

@Composable
private fun HeaviestWeightLineChart(
    points: List<Float>,
    unitLabel: String,
    lineColor: androidx.compose.ui.graphics.Color,
    axisColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val max = points.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val min = points.minOrNull() ?: 0f
    val range = (max - min).takeIf { it > 0f } ?: 1f

    // Nielsen-ish: visibility of status -> show top value label
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

            // Axes (subtle)
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

            // Points
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
