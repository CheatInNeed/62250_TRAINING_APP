package com.example.gymlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.AppRoot
import com.example.gymlocker.ui.theme.GymLockerTheme
import kotlinx.coroutines.launch
import com.example.gymlocker.data.dao.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        lifecycleScope.launch {
            //AppDatabase.prepopulate(applicationContext)
        }

        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}
