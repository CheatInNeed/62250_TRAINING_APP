package com.example.gymlocker.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.R
import com.example.gymlocker.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun SplashScreen(
    navController: NavController
) {
    val context = LocalContext.current

    // Simple logo/text entrance animation
    val scale = remember { Animatable(0.92f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Run animation in parallel with loading
        val animJob = launch {
            scale.animateTo(1f, animationSpec = tween(durationMillis = 600))
        }
        val alphaJob = launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 450))
        }

        // --- "Real" loading work (warm up DB etc.) ---
        val minShowMs = 1000L
        val start = System.currentTimeMillis()

        // Keep this fast: just ensure DB instance is created.
        // If you want to be stricter, you can do a lightweight query.
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            // Optional stricter readiness check (uncomment if you want):
            // db.userDao().countUsers()
        }


        val elapsed = System.currentTimeMillis() - start
        val remaining = max(0L, minShowMs - elapsed)
        if (remaining > 0) kotlinx.coroutines.delay(remaining)

        animJob.join()
        alphaJob.join()

        // Navigate away
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
                .padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.gymlocker_logo),
                contentDescription = "GymLocker logo",
                modifier = Modifier.size(360.dp)
            )

            Spacer(Modifier.height(16.dp))


            Text(
                text = "Track. Progress. Repeat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            CircularProgressIndicator()
        }
    }
}
