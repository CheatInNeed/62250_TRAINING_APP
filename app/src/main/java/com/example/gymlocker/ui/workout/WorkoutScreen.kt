package com.example.gymlocker.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.theme.BotBarShape
import com.example.gymlocker.ui.theme.TopBarShape
import com.example.gymlocker.ui.theme.metalGloss
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    navController: NavController,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val isWorkoutInProgress by activeWorkoutViewModel.isWorkoutInProgress.collectAsState()

    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)

    val templatesFlow = remember(activeProfileUserId) {
        activeWorkoutViewModel.observeTemplates(activeProfileUserId)
    }
    val templates by templatesFlow.collectAsState(initial = emptyList())
    var showBrowseTemplatesSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.metalGloss(TopBarShape),
                title = { Text("Workout") },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Top controls: create template / exercise
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate("createTemplate") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = activeProfileUserId != null,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Create Template")
                    }

                    OutlinedButton(
                        onClick = { navController.navigate("createExercise") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = activeProfileUserId != null,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Create Exercise")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Categorize templates
                // Gymlocker templates are identified by "(Dummy)" in the name
                val gymlockerTemplates = templates.filter { it.name.contains("(Dummy)", ignoreCase = true) }
                val userCreatedTemplates = templates.filter { !it.name.contains("(Dummy)", ignoreCase = true) }
                val favoriteTemplates = templates.filter { it.isFavorite }
                val yourTemplates = userCreatedTemplates.filter { it.userId == activeProfileUserId }

            val templateSummaries = remember { mutableStateMapOf<Long, String>() }

            LaunchedEffect(templates) {
                val db = AppDatabase.getDatabase(context)
                withContext(Dispatchers.IO) {
                    for (t in templates) {
                        if (templateSummaries.containsKey(t.templateId)) continue

                        val tpl = db.workoutTemplateDao().getTemplateWithExercises(t.templateId)
                        if (tpl == null) {
                            templateSummaries[t.templateId] = "No exercises"
                            continue
                        }

                        val exerciseCount = tpl.exercises.size
                        val totalSets = tpl.exercises.sumOf { it.sets.size }

                        templateSummaries[t.templateId] =
                            if (exerciseCount == 0) "No exercises"
                            else "$exerciseCount exercises x $totalSets sets"
                    }
                }
            }

            // Browse all templates button
            TextButton(
                onClick = { showBrowseTemplatesSheet = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Browse templates",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text("Browse All Templates")
            }

            Spacer(Modifier.height(12.dp))

            // Expandable sections
            ExpandableTemplateSection(
                title = "Favorite Templates",
                templates = favoriteTemplates,
                templateSummaries = templateSummaries,
                navController = navController,
                initiallyExpanded = true
            )

            Spacer(Modifier.height(8.dp))

            ExpandableTemplateSection(
                title = "Your Templates",
                templates = yourTemplates,
                templateSummaries = templateSummaries,
                navController = navController,
                initiallyExpanded = false
            )

            Spacer(Modifier.height(8.dp))

            ExpandableTemplateSection(
                title = "Gymlocker's Templates",
                templates = gymlockerTemplates,
                templateSummaries = templateSummaries,
                navController = navController,
                initiallyExpanded = false
            )

            Spacer(Modifier.height(16.dp))
        }

        // Fixed button at bottom
        Button(
            onClick = {
                // Resume if in progress, otherwise start new (ActiveWorkoutScreen handles it)
                navController.navigate("activeWorkout")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = activeProfileUserId != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(if (isWorkoutInProgress) "Resume Workout" else "Start Empty Workout")
        }
    }

        if (showBrowseTemplatesSheet) {
            TemplateBrowseSheet(
                templates = templates,
                onDismiss = { showBrowseTemplatesSheet = false },
                onTemplateSelected = { templateId ->
                    navController.navigate("templateDetail/$templateId")
                    showBrowseTemplatesSheet = false
                }
            )
        }
    }
}

@Composable
fun ExpandableTemplateSection(
    title: String,
    templates: List<com.example.gymlocker.data.entity.template.WorkoutTemplate>,
    templateSummaries: Map<Long, String>,
    navController: NavController,
    initiallyExpanded: Boolean = false
) {
    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow rotation"
    )

    Column {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .metalGloss(RoundedCornerShape(12.dp))
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${templates.size} template${if (templates.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp)
            ) {
                if (templates.isEmpty()) {
                    Text(
                        text = "No templates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // Grid of template cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        templates.chunked(2).forEach { rowTemplates ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowTemplates.forEach { t ->
                                    val templateCardShape = RoundedCornerShape(18.dp)

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(96.dp)
                                            .metalGloss(templateCardShape)
                                            .clickable { navController.navigate("templateDetail/${t.templateId}") },
                                        shape = templateCardShape,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Column(Modifier.padding(14.dp)) {
                                            Text(
                                                text = t.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 2,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            val summary = templateSummaries[t.templateId] ?: "Loading..."

                                            Text(
                                                text = summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                                // Fill empty space if odd number of templates
                                if (rowTemplates.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
