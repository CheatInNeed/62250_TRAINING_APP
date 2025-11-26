package com.example.gymlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.ui.AppNavigation
import com.example.gymlocker.ui.ViewModelFactory
import com.example.gymlocker.ui.theme.GymLockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppDatabase.getDatabase(this)
        val viewModelFactory = ViewModelFactory(database.userDao(), database.workoutDao(), database.exerciseDao(), database.exerciseLogDao(), database.userWorkoutCrossRefDao(), database.muscleGroupDao())
        setContent {
            GymLockerTheme {
                AppNavigation(viewModelFactory)
            }
        }
    }
}
