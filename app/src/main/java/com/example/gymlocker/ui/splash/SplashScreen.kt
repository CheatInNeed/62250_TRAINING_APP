package com.example.gymlocker.ui.splash

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
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
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        // TODO: Change this value to determine loading min time
        val minShowMs = 50L
        val start = System.currentTimeMillis()

        // Keep this fast: just ensure DB instance is created.
        // If you want to be stricter, you can do a lightweight query.
        // --- Real loading work ---
        val ok = withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)

                // 1) Sanity check: DB can execute SQL
                db.openHelper.writableDatabase.query("SELECT 1").use { /* no-op */ }

                // 2) SQLite quick integrity check (fast)
                db.openHelper.writableDatabase.query("PRAGMA quick_check(1)").use { cursor ->
                    if (cursor.moveToFirst()) {
                        val result = cursor.getString(0) // "ok" if healthy
                        if (result != "ok") {
                            Log.e("SplashScreen", "SQLite quick_check failed: $result")
                            return@withContext false
                        }
                    }
                }

                // 3) Ensure required reference data exists (example)
                // NOTE: Replace these with your actual DAO/entity methods.
                // If you already seed elsewhere, you can skip this.
                // Example pattern:
                // val mgDao = db.muscleGroupDao()
                // if (mgDao.count() == 0) mgDao.insertAll(defaultMuscleGroups)

                true
            } catch (t: Throwable) {
                Log.e("SplashScreen", "DB warmup failed", t)
                false
            }
        }

        val elapsed = System.currentTimeMillis() - start
        val remaining = max(0L, minShowMs - elapsed)
        if (remaining > 0) kotlinx.coroutines.delay(remaining)

        animJob.join()
        alphaJob.join()

        // Navigate away
        val session = SessionManager(context.applicationContext)
        val loggedIn = session.isLoggedIn.first()

        val target = if (loggedIn) "workout" else "login"

        navController.navigate(target) {
            popUpTo("splash") { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
                painter = painterResource(id = R.drawable.gymlocker_logo_new_transparent),
                contentDescription = "GymLocker logo",
                modifier = Modifier.size(360.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Track. Progress. Repeat.",
                style = MaterialTheme.typography.bodyMedium,
                // Splash is a screen layer; use onBackground for default text
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(20.dp))

            CircularProgressIndicator(
                // Explicitly set to avoid theme surprises; progress indicators are accents
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
