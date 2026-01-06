package com.example.gymlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.AppNavigation
import com.example.gymlocker.ui.theme.GymLockerTheme
import kotlinx.coroutines.launch
import com.example.gymlocker.data.dao.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            //AppDatabase.prepopulate(applicationContext)
        }

        enableEdgeToEdge()
        setContent {
            GymLockerTheme {
                AppNavigation()
            }
        }
    }
}
