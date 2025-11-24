package com.example.gymlocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.gymlocker.ui.home.HomeScreen
import com.example.gymlocker.ui.workout.NewWorkoutScreen
import com.example.gymlocker.ui.workout.WorkoutScreen
import com.example.gymlocker.ui.workout.WorkoutSummaryScreen

@Composable
fun AppNavigation(viewModelFactory: ViewModelFactory) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("workout") { WorkoutScreen(navController) }
        navigation(startDestination = "new_workout_screen", route = "new_workout") {
            composable("new_workout_screen") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("new_workout")
                }
                NewWorkoutScreen(navController, viewModel(parentEntry, factory = viewModelFactory))
            }
            composable("workout_summary") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("new_workout")
                }
                WorkoutSummaryScreen(navController, viewModel(parentEntry, factory = viewModelFactory))
            }
        }
    }
}
