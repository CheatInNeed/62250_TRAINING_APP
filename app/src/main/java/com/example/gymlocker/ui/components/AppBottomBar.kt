package com.example.gymlocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add

@Composable
fun AppBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val homeRoutes = setOf("home")
    val workoutRoutes = setOf(
        "workout",
        "activeWorkout",
        "createTemplate",
        "workoutHistory",
        "workoutDetail/{workoutId}",
        "templateDetail/{templateId}"
    )

    val isProfileSelected = currentRoute == "profile"
    val isWorkoutSelected = !isProfileSelected && (currentRoute in workoutRoutes)
    val isHomeSelected = !isProfileSelected && !isWorkoutSelected && (currentRoute in homeRoutes)

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // ✅ System colors for nav icons
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)

        Row(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // HOME (venstre)
            IconButton(
                onClick = {
                    if (currentRoute != "home") {
                        navController.navigate("home") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo("home") { inclusive = false }
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home",
                    tint = if (isHomeSelected) selectedColor else unselectedColor
                )
            }

            // WORKOUT (midten) — dynamisk ikon
            val isOnWorkoutPage = currentRoute == "workout"

            IconButton(
                onClick = {
                    if (isOnWorkoutPage) {
                        // På workout-siden: start tom workout
                        navController.navigate("activeWorkout") { launchSingleTop = true }
                    } else {
                        // Alle andre sider: gå til workout-siden
                        navController.navigate("workout") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            ) {
                if (isOnWorkoutPage) {
                    // Primary circle + onPrimary plus (matches system)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Start Workout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    // Normal dumbbell ikon
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "Workout",
                        tint = if (isWorkoutSelected) selectedColor else unselectedColor
                    )
                }
            }

            // PROFILE (højre)
            IconButton(
                onClick = {
                    if (currentRoute != "profile") {
                        navController.navigate("profile") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = if (isProfileSelected) selectedColor else unselectedColor
                )
            }
        }
    }
}
