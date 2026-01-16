package com.example.gymlocker.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
import com.example.gymlocker.ui.components.ProfileAvatarIcon
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.gymlocker.data.auth.SessionManager
import androidx.navigation.compose.currentBackStackEntryAsState


@SuppressLint("UnrememberedMutableState")
@Composable
fun AppBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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

    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }

    val activeProfileUserId by session.activeProfileUserId.collectAsState(initial = null)

    // Photo uri for the active profile (null if no profile or no photo)
    val activeProfilePhotoUri by (
            if (activeProfileUserId != null)
                session.profilePhotoUri(activeProfileUserId!!).collectAsState(initial = null)
            else
                mutableStateOf<String?>(null)
            )

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // HOME (left)
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

            // WORKOUT (middle) — dynamic icon
            val isOnWorkoutPage = currentRoute == "workout"

            IconButton(
                onClick = {
                    if (isOnWorkoutPage) {
                        // On workout page: start empty workout
                        navController.navigate("activeWorkout") { launchSingleTop = true }
                    } else {
                        // Elsewhere: go to workout page
                        navController.navigate("workout") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            ) {
                if (isOnWorkoutPage) {
                    // Primary circle + onPrimary plus (CTA)
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
                    // Normal dumbbell icon
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "Workout",
                        tint = if (isWorkoutSelected) selectedColor else unselectedColor
                    )
                }
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
                // Avatar (fallback to person icon inside ProfileAvatarIcon)
                ProfileAvatarIcon(
                    uriString = activeProfilePhotoUri,
                    size = 26.dp,
                    modifier = Modifier
                        .then(
                            if (isProfileSelected)
                                Modifier.border(2.dp, selectedColor, CircleShape)
                            else
                                Modifier
                        )
                )
            }

        }
    }
}
