package com.example.gymlocker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, activeWorkoutViewModel: ActiveWorkoutViewModel) {
    val isWorkoutInProgress by activeWorkoutViewModel.isWorkoutInProgress.collectAsState()
    val completedWorkouts by activeWorkoutViewModel.completedWorkouts().collectAsState(initial = emptyList())
    val lastWorkoutLabel by activeWorkoutViewModel.lastWorkoutLabel().collectAsState(initial = "Finder seneste workout…")

    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)

    // --- Query workouts in current week (Mon–Sun) from ExerciseLogDao (only "completed" workouts) ---
    val db = remember { AppDatabase.getDatabase(context) }
    val exerciseLogDao = remember { db.exerciseLogDao() }

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") }
    val today = LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val endOfWeek = startOfWeek.plusDays(6)

    val startInclusive = startOfWeek.atStartOfDay().format(formatter)
    val endInclusive = endOfWeek.atTime(23, 59, 59, 999_000_000).format(formatter)

    val workoutsThisWeek by exerciseLogDao
        .observeCompletedWorkoutCountInRange(startInclusive = startInclusive, endInclusive = endInclusive)
        .collectAsState(initial = 0)
    // ---------------------------------------------------------------------------------------------

    val templatesFlow = remember(activeProfileUserId) {
        activeWorkoutViewModel.observeTemplates(activeProfileUserId)
    }
    val templates by templatesFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) },
        bottomBar = {
            Column {
                Button(
                    onClick = {
                        if (isWorkoutInProgress) navController.navigate("activeWorkout")
                        else navController.navigate("workout")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = activeProfileUserId != null
                ) {
                    Text(if (isWorkoutInProgress) "Resume Workout" else "Start Workout")
                }

                ActiveWorkoutBanner(navController, activeWorkoutViewModel)
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->

        // Optional UX hint until profile flow exists
        if (activeProfileUserId == null) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No profile selected.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Phase 2 will add: Create Profile + pick active profile.\nFor now templates and workouts are disabled.",
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        // Your existing home content continues here...
        // (I’m keeping the rest minimal because your pasted HomeScreen was truncated in the big TXT.)
    }
}
