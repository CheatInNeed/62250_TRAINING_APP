package com.example.gymlocker.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymlocker.data.entity.template.WorkoutTemplate
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalContext
import com.example.gymlocker.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateBrowseSheet(
    templates: List<WorkoutTemplate>,
    onDismiss: () -> Unit,
    onTemplateSelected: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(templates, searchQuery) {
        templates
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(compareByDescending<WorkoutTemplate> { it.isFavorite }
                .thenByDescending { it.templateId })
    }

    val context = LocalContext.current
    val templateSummaries = remember { mutableStateMapOf<Long, String>() }

    LaunchedEffect(filtered) {
        val db = AppDatabase.getDatabase(context)
        withContext(Dispatchers.IO) {
            for (t in filtered) {
                if (templateSummaries.containsKey(t.templateId)) continue

                val tpl = db.workoutTemplateDao().getTemplateWithExercises(t.templateId)
                if (tpl == null) {
                    templateSummaries[t.templateId] = "No exercises"
                    continue
                }

                val exerciseCount = tpl.exercises.size
                val setCount = tpl.exercises.sumOf { it.sets.size }

                templateSummaries[t.templateId] =
                    if (exerciseCount == 0) "No exercises"
                    else "$exerciseCount exercises • $setCount sets"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text("Browse Templates", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Search templates")
                    }
                },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { t ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTemplateSelected(t.templateId) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT:
                            Column(modifier = Modifier.weight(1f)) {

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = t.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (t.isFavorite) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Favorite",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFFFFC107) // gul
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                val summary = templateSummaries[t.templateId] ?: "Loading..."
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // RIGHT
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
