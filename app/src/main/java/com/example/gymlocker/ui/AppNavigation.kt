package com.example.gymlocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymlocker.ui.activeworkout.ActiveWorkoutScreen
import com.example.gymlocker.ui.history.WorkoutHistoryScreen
import com.example.gymlocker.ui.home.HomeScreen
import com.example.gymlocker.ui.template.CreateTemplateScreen
import com.example.gymlocker.ui.template.TemplateDetailScreen
import com.example.gymlocker.ui.workout.WorkoutDetailScreen
import com.example.gymlocker.ui.workout.WorkoutScreen
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.WorkoutHistoryViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        factory = ActiveWorkoutViewModel.provideFactory(context)
    )

    val historyViewModel: WorkoutHistoryViewModel = viewModel(
        factory = WorkoutHistoryViewModel.provideFactory(context)
    )

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, activeWorkoutViewModel) }
        composable("workout") { WorkoutScreen(navController) }
        composable("activeWorkout") { ActiveWorkoutScreen(navController, activeWorkoutViewModel) }
        composable("createTemplate") { CreateTemplateScreen(navController) }
        composable("workoutHistory") { WorkoutHistoryScreen(navController, historyViewModel) }
        composable(
            route = "workoutDetail/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
            WorkoutDetailScreen(workoutId, navController, historyViewModel)
        }
        composable(
            route = "templateDetail/{templateId}",
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
            TemplateDetailScreen(templateId, navController, activeWorkoutViewModel)
        }
    }
}
