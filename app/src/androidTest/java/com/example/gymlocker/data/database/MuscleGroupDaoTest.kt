package com.example.gymlocker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymlocker.data.dao.MuscleGroupDao
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
class MuscleGroupDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var muscleGroupDao: MuscleGroupDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        muscleGroupDao = db.muscleGroupDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeMuscleGroupAndReadInList() = runBlocking {
        val muscleGroup = MuscleGroup(muscleGroupId = 1, name = "Test Muscle Group")
        muscleGroupDao.insert(muscleGroup)
        val allMuscleGroups = muscleGroupDao.getAllMuscleGroups().first()
        assertEquals(allMuscleGroups[0], muscleGroup)
    }
}
