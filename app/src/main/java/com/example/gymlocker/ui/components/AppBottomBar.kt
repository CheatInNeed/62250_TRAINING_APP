package com.example.gymlocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
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

@Composable
fun AppBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    // "Home area" routes = highlight Home icon, even when not literally on "home"
    val homeRoutes = setOf(
        "home",
        "workout",
        "activeWorkout",
        "createTemplate",
        "workoutHistory",
        "workoutDetail/{workoutId}",
        "templateDetail/{templateId}"
    )

    val isProfileSelected = currentRoute == "profile"
    val isHomeSelected = !isProfileSelected && (currentRoute in homeRoutes)

    BottomAppBar {
        Row(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // ✅ Always go to the actual Home screen when tapped (unless already there)
            IconButton(
                onClick = {
                    if (currentRoute != "home") {
                        navController.navigate("home") {
                            launchSingleTop = true
                            restoreState = true
                            // Pop back stack to home if it exists, otherwise just navigate
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
