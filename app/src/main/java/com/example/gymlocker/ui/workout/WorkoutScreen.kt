package com.example.gymlocker.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.ui.components.ActiveWorkoutBanner
import com.example.gymlocker.ui.components.AppBottomBar
import com.example.gymlocker.ui.home.TemplatesCard
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel

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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Workout") }) },
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
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    // Resume hvis i gang, ellers start ny (din ActiveWorkoutScreen håndterer det)
                    navController.navigate("activeWorkout")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = activeProfileUserId != null
            ) {
                Text(if (isWorkoutInProgress) "Resume Workout" else "Start Workout")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("createTemplate") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = activeProfileUserId != null
            ) {
                Text("Opret ny template")
            }

            Spacer(Modifier.height(16.dp))

            TemplatesCard(
                templates = templates,
                onStartFromTemplate = { templateId ->
                    navController.navigate("templateDetail/$templateId")
                }
            )
        }
    }
}
