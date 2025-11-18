package com.example.gymlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.gymlocker.ui.AppNavigation
import com.example.gymlocker.ui.theme.GymLockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymLockerTheme {
                AppNavigation()
            }
        }
    }
}
