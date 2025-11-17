package com.example.gymlocker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymlocker.data.dao.ExerciseDao
import com.example.gymlocker.data.dao.ExerciseLogDao
import com.example.gymlocker.data.dao.MuscleGroupDao
import com.example.gymlocker.data.entity.ExerciseLog
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ExerciseLogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var muscleGroupDao: MuscleGroupDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseLogDao: ExerciseLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        muscleGroupDao = db.muscleGroupDao()
        exerciseDao = db.exerciseDao()
        exerciseLogDao = db.exerciseLogDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeLogAndReadInList() = runBlocking {
        val muscleGroup = MuscleGroup(muscleGroupId = 1, name = "Test Muscle Group")
        muscleGroupDao.insert(muscleGroup)
        val exercise = Exercises(exerciseId = 1, name = "Test Exercise", startWeight = 10, startReps = 10, isRecent = true, muscleGroupId = 1)
        exerciseDao.insert(exercise)
        val log = ExerciseLog(logId = 1, exerciseId = 1, reps = 10, weight = 10, date = "2024-01-01")
        exerciseLogDao.insert(log)
        val logs = exerciseLogDao.getLogsForExercise(1).first()
        assertEquals(logs[0], log)
    }
}
