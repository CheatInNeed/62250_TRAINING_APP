package com.example.gymlocker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymlocker.data.dao.UserDao
import com.example.gymlocker.data.dao.WorkoutDao
import com.example.gymlocker.data.entity.User
import com.example.gymlocker.data.entity.Workout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        userDao = db.userDao()
        workoutDao = db.workoutDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeWorkoutAndReadInList() = runBlocking {
        val user = User(userId = 1, name = "Test User", height = 180, weight = 80)
        userDao.insert(user)
        val workout = Workout(workoutId = 1, name = "Test Workout", date = "2024-01-01", userId = 1)
        workoutDao.insert(workout)
        val workouts = workoutDao.getWorkoutsWithExercises(1).first()
        assertEquals(workouts[0].workout, workout)
    }
}
