package com.example.gymlocker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gymlocker.MainActivity
import com.example.gymlocker.R

class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: "Next set"

        ensureChannel(context)

        // Tap på notifikation -> åbner appen
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "activeWorkout") // valgfrit (hvis I vil deep-linke senere)
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // brug jeres egen hvis I har en bedre
            .setContentTitle("Rest færdig")
            .setContentText("$exerciseName: klar til næste sæt 💪")
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true) // gør den “let at dismiss”
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Android 13+: kræver runtime-permission (se trin 4)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = mgr.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rest timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikation når din rest timer er færdig."
        }

        mgr.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val NOTIFICATION_ID = 1403

        const val EXTRA_EXERCISE_NAME = "extra_exercise_name"
    }
}
