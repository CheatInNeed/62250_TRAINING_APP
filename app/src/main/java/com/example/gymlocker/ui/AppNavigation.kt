package com.example.gymlocker.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.ui.activeworkout.ActiveWorkoutScreen
import com.example.gymlocker.ui.home.HomeScreen
import com.example.gymlocker.ui.workout.WorkoutScreen
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, activeWorkoutViewModel) }
        composable("workout") { WorkoutScreen(navController) }
        composable("activeWorkout") { ActiveWorkoutScreen(navController, activeWorkoutViewModel) }
    }
}
