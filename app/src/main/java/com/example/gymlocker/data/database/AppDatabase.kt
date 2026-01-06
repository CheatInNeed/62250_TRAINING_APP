package com.example.gymlocker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gymlocker.data.dao.*
import com.example.gymlocker.data.dao.template.*
import com.example.gymlocker.data.entity.*
import com.example.gymlocker.data.entity.template.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Database(
    entities = [
        User::class,
        Workout::class,
        MuscleGroup::class,
        Exercises::class,
        ExerciseLog::class,
        PerformedSet::class,

        // ✅ Templates (Option A)
        WorkoutTemplate::class,
        TemplateExercise::class,
        TemplateSet::class
    ],
    version = 2,
    // ✅ Avoid Room kapt failing unless you ALSO configure room.schemaLocation in Gradle.
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun performedSetDao(): PerformedSetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "gymlocker.db"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                // ✅ Always rebuild DB from scratch on every app start
                deleteDatabaseFiles(context, DB_NAME)

                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    )
                        .fallbackToDestructiveMigration()
                        .addCallback(SeedCallback())
                        .build()

                INSTANCE = instance
                instance
            }
        }

        private fun deleteDatabaseFiles(context: Context, dbName: String) {
            // Main DB
            context.deleteDatabase(dbName)

            // Room sidecar files (best-effort cleanup)
            val dbFile = context.getDatabasePath(dbName)
            val shm = File(dbFile.path + "-shm")
            val wal = File(dbFile.path + "-wal")
            if (shm.exists()) shm.delete()
            if (wal.exists()) wal.delete()
        }
    }

    /**
     * Seeds default data once after DB creation.
     * This callback is safe (it uses the DB instance that was built, not INSTANCE?).
     */
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Use a background coroutine and fetch the instance safely.
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.seedIfEmpty()
                }
            }
        }
    }

    /**
     * Only inserts seed data if the DB looks empty.
     * Adjust the count() calls to match your DAO APIs.
     */
    private suspend fun seedIfEmpty() {
        // If your DAOs don’t have these count methods, either add them or change the condition.
        val userDao = userDao()
        val exerciseDao = exerciseDao()
        val muscleGroupDao = muscleGroupDao()

        // --- Guard: don't seed twice ---
        // If you don't have countUsers/countExercises, remove these checks and just seed.
        val usersCount = runCatching { userDao.countUsers() }.getOrNull()
        val exercisesCount = runCatching { exerciseDao.countExercises() }.getOrNull()

        if ((usersCount != null && usersCount > 0) || (exercisesCount != null && exercisesCount > 0)) return

        // --- Seed user ---
        // Prefer letting Room autogenerate ID unless you *need* userId=1.
        userDao.insert(User(name = "Default User", height = 0, weight = 0))

        // --- Seed muscle groups ---
        val chestId = muscleGroupDao.insert(MuscleGroup(name = "Chest"))
        val legsId = muscleGroupDao.insert(MuscleGroup(name = "Legs"))
        val backId = muscleGroupDao.insert(MuscleGroup(name = "Back"))
        val shouldersId = muscleGroupDao.insert(MuscleGroup(name = "Shoulders"))
        val armsId = muscleGroupDao.insert(MuscleGroup(name = "Arms"))

        // --- Seed exercises ---
        exerciseDao.insert(
            Exercises(
                name = "Bench Press",
                startWeight = 0,
                startReps = 0,
                isRecent = true,
                muscleGroupId = chestId
            )
        )
        exerciseDao.insert(
            Exercises(
                name = "Squat",
                startWeight = 0,
                startReps = 0,
                isRecent = true,
                muscleGroupId = legsId
            )
        )
        exerciseDao.insert(
            Exercises(
                name = "Deadlift",
                startWeight = 0,
                startReps = 0,
                isRecent = false,
                muscleGroupId = backId
            )
        )
        exerciseDao.insert(
            Exercises(
                name = "Overhead Press",
                startWeight = 0,
                startReps = 0,
                isRecent = false,
                muscleGroupId = shouldersId
            )
        )
        exerciseDao.insert(
            Exercises(
                name = "Barbell Row",
                startWeight = 0,
                startReps = 0,
                isRecent = false,
                muscleGroupId = backId
            )
        )
        exerciseDao.insert(
            Exercises(
                name = "Pull-up",
                startWeight = 0,
                startReps = 0,
                isRecent = false,
                muscleGroupId = backId
            )
        )
        exerciseDao.insert(
            Exercises(
                name = "Bicep Curl",
                startWeight = 0,
                startReps = 0,
                isRecent = false,
                muscleGroupId = armsId
            )
        )
    }
}