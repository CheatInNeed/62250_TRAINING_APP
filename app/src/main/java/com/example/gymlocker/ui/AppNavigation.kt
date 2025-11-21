package com.example.gymlocker.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.ui.activeworkout.ActiveWorkoutScreen
import com.example.gymlocker.ui.home.HomeScreen
import com.example.gymlocker.ui.workout.WorkoutScreen
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.WorkoutViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { 
            val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()
            HomeScreen(navController, activeWorkoutViewModel)
        }
        composable("workout") { WorkoutScreen(navController) }
        composable("activeWorkout") { 
            val workoutViewModel: WorkoutViewModel = hiltViewModel()
            val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()
            ActiveWorkoutScreen(navController, workoutViewModel, activeWorkoutViewModel)
        }
    }
}
