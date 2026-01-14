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
import com.example.gymlocker.ui.profile.EditProfileScreen
import com.example.gymlocker.ui.profile.ProfileScreen
import com.example.gymlocker.ui.splash.SplashScreen
import com.example.gymlocker.ui.template.CreateTemplateScreen
import com.example.gymlocker.ui.template.EditTemplateScreen
import com.example.gymlocker.ui.template.TemplateDetailScreen
import com.example.gymlocker.ui.workout.CreateExerciseScreen
import com.example.gymlocker.ui.workout.WorkoutDetailScreen
import com.example.gymlocker.ui.workout.WorkoutScreen
import com.example.gymlocker.viewmodel.CreateExerciseViewModel
import com.example.gymlocker.viewmodel.ActiveWorkoutViewModel
import com.example.gymlocker.viewmodel.AuthViewModel
import com.example.gymlocker.viewmodel.CreateTemplateViewModel
import com.example.gymlocker.viewmodel.EditTemplateViewModel
import com.example.gymlocker.viewmodel.ProfileViewModel
import com.example.gymlocker.viewmodel.WorkoutHistoryViewModel
import com.example.gymlocker.ui.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // ViewModels (single instances scoped to this Nav graph / composition)
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
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(context)
    )
    val createExerciseViewModel: CreateExerciseViewModel = viewModel(
        factory = CreateExerciseViewModel.provideFactory(context)
    )

    // Decide start destination BEFORE NavHost
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)

    // If you truly need to wait for a "real" auth state (instead of initial=false),
    // you can add an "isAuthLoaded" in AuthViewModel. For now we keep it simple.
    val startDestination = if (isLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash decides where to go next (recommended)
        composable("splash") {
            SplashScreen(navController = navController)
        }

        // Auth
        composable("login") {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }

        // Main
        composable("home") {
            HomeScreen(navController = navController, activeWorkoutViewModel = activeWorkoutViewModel)
        }
        composable("workout") {
            WorkoutScreen(navController = navController, activeWorkoutViewModel = activeWorkoutViewModel)
        }
        composable("createExercise") {
            CreateExerciseScreen(
                navController = navController,
                viewModel = createExerciseViewModel
            )
        }
        composable("activeWorkout") {
            ActiveWorkoutScreen(
                navController = navController,
                viewModel = activeWorkoutViewModel
            )
        }
        composable("workoutHistory") {
            WorkoutHistoryScreen(
                navController = navController,
                viewModel = historyViewModel,
                activeWorkoutViewModel = activeWorkoutViewModel
            )
        }

        // Templates
        composable("createTemplate") {
            CreateTemplateScreen(
                navController = navController,
                viewModel = createTemplateViewModel,
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

        composable(
            route = "editTemplate/{templateId}",
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
            val editTemplateViewModel: EditTemplateViewModel = viewModel(
                factory = EditTemplateViewModel.provideFactory(context, templateId)
            )
            EditTemplateScreen(
                templateId = templateId,
                navController = navController,
                viewModel = editTemplateViewModel,
                activeWorkoutViewModel = activeWorkoutViewModel
            )
        }

        // Profile
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
        composable("editProfile") {
            EditProfileScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }
        composable("settings") {
            SettingsScreen(navController)
        }


        // Workout detail
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
    }

    // Optional: If you *don't* want SplashScreen to decide, you can auto-redirect once here.
    // But then SplashScreen should just be a static UI.
    LaunchedEffect(isLoggedIn) {
        // Only redirect if we're at splash (or if you want to force-correct current route)
        // If you have a real splash flow, do navigation inside SplashScreen instead.
        // navController.navigate(startDestination) {
        //     popUpTo("splash") { inclusive = true }
        // }
    }
}
