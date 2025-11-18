package com.example.gymlocker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymlocker.ui.home.HomeScreen
import com.example.gymlocker.ui.workout.WorkoutScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("workout") { WorkoutScreen() }
    }
}
