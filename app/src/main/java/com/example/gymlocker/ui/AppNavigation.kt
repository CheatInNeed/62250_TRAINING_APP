package com.example.gymlocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.gymlocker.ui.addexercise.AddExerciseScreen
import com.example.gymlocker.ui.activeworkout.ActiveWorkoutScreen
import com.example.gymlocker.ui.home.HomeScreen
import com.example.gymlocker.ui.workout.NewWorkoutScreen
import com.example.gymlocker.ui.workout.NewWorkoutViewModel
import com.example.gymlocker.ui.workout.WorkoutScreen
import com.example.gymlocker.ui.workout.WorkoutSummaryScreen
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel

@Composable
fun AppNavigation(viewModelFactory: ViewModelFactory) {
    val navController = rememberNavController()
    val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, activeWorkoutViewModel) }
        composable("workout") { WorkoutScreen(navController, activeWorkoutViewModel) }

        navigation(startDestination = "active_workout_screen", route = "activeWorkout") {
            composable("active_workout_screen") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("activeWorkout")
                }
                ActiveWorkoutScreen(navController, viewModel(parentEntry))
            }
            composable("add_exercise_active") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("activeWorkout")
                }
                val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(parentEntry)
                AddExerciseScreen(navController) { exerciseName ->
                    activeWorkoutViewModel.addExercise(exerciseName)
                }
            }
        }

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
            composable("add_exercise") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("new_workout")
                }
                val newWorkoutViewModel: NewWorkoutViewModel = viewModel(parentEntry, factory = viewModelFactory)
                AddExerciseScreen(navController) { exerciseName ->
                    newWorkoutViewModel.addExercise(exerciseName)
                }
            }
        }
    }
}
