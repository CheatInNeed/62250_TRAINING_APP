package com.example.gymlocker.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymlocker.ui.activeworkout.ActiveWorkoutScreen
import com.example.gymlocker.ui.auth.LoginScreen
import com.example.gymlocker.ui.auth.RegisterScreen
import com.example.gymlocker.ui.history.WorkoutHistoryScreen
import com.example.gymlocker.ui.home.HomeScreen
import com.example.gymlocker.ui.profile.CreateProfileScreen
import com.example.gymlocker.ui.profile.ProfileScreen
import com.example.gymlocker.ui.template.CreateTemplateScreen
import com.example.gymlocker.ui.template.TemplateDetailScreen
import com.example.gymlocker.ui.workout.WorkoutDetailScreen
import com.example.gymlocker.ui.workout.WorkoutScreen
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.AuthViewModel
import com.example.gymlocker.viewmodel.CreateTemplateViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import com.example.gymlocker.viewmodel.WorkoutHistoryViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        factory = ActiveWorkoutViewModel.provideFactory(context)
    )

    val createTemplateViewModel: CreateTemplateViewModel = viewModel(
        factory = CreateTemplateViewModel.provideFactory(context)
    )

    val historyViewModel: WorkoutHistoryViewModel = viewModel(
        factory = WorkoutHistoryViewModel.provideFactory(context)
    )

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.provideFactory(context)
    )

    // ✅ CREATE IT (this was missing)
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(context)
    )

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLoggedIn) {
        if (startDestination == null) {
            startDestination = if (isLoggedIn) "home" else "login"
        }
    }

    if (startDestination == null) return

    NavHost(navController = navController, startDestination = startDestination!!) {

        composable("login") {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }

        composable("register") {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }

        composable("home") {
            HomeScreen(navController, activeWorkoutViewModel)
        }

        composable("workout") {
            WorkoutScreen(navController)
        }

        composable("activeWorkout") {
            ActiveWorkoutScreen(navController, activeWorkoutViewModel)
        }

        composable("createTemplate") {
            CreateTemplateScreen(
                navController = navController,
                viewModel = createTemplateViewModel,
                activeWorkoutViewModel = activeWorkoutViewModel
            )
        }

        composable("workoutHistory") {
            WorkoutHistoryScreen(
                navController = navController,
                viewModel = historyViewModel,
                activeWorkoutViewModel = activeWorkoutViewModel
            )
        }

        composable("profile") {
            ProfileScreen(
                navController = navController,
                authViewModel = authViewModel,
                activeWorkoutViewModel = activeWorkoutViewModel,
                profileViewModel = profileViewModel
            )
        }

        composable("createProfile") {
            CreateProfileScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }


        composable(
            route = "workoutDetail/{workoutId}",
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
            WorkoutDetailScreen(
                workoutId = workoutId,
                navController = navController,
                viewModel = historyViewModel,
                activeWorkoutViewModel = activeWorkoutViewModel
            )
        }

        composable(
            route = "templateDetail/{templateId}",
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
            TemplateDetailScreen(
                templateId = templateId,
                navController = navController,
                activeWorkoutViewModel = activeWorkoutViewModel
            )
        }
    }
}
