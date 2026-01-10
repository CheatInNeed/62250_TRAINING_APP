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
        // Profile (existing)
        User::class,

        // Auth (NEW)
        AuthAccount::class,

        Workout::class,
        MuscleGroup::class,
        Exercises::class,
        ExerciseLog::class,
        PerformedSet::class,

        // Templates
        WorkoutTemplate::class,
        TemplateExercise::class,
        TemplateSet::class
    ],
    version = 3, // <-- bumped from 2 -> 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    // NEW
    abstract fun authAccountDao(): AuthAccountDao

    abstract fun workoutDao(): WorkoutDao
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun performedSetDao(): PerformedSetDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun templateExerciseDao(): TemplateExerciseDao
    abstract fun templateSetDao(): TemplateSetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "gymlocker.db"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                // ⚠️ You currently delete the DB every app start.
                // That makes "persist login between app sessions" impossible in practice,
                // because user accounts will be removed.
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
            context.deleteDatabase(dbName)

            val dbFile = context.getDatabasePath(dbName)
            val shm = File(dbFile.path + "-shm")
            val wal = File(dbFile.path + "-wal")
            if (shm.exists()) shm.delete()
            if (wal.exists()) wal.delete()
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.seedIfEmpty()
                }
            }
        }
    }

    private suspend fun seedIfEmpty() {
        val userDao = userDao()
        val exerciseDao = exerciseDao()
        val muscleGroupDao = muscleGroupDao()

        val usersCount = runCatching { userDao.countUsers() }.getOrNull()
        val exercisesCount = runCatching { exerciseDao.countExercises() }.getOrNull()

        if ((usersCount != null && usersCount > 0) || (exercisesCount != null && exercisesCount > 0)) return

        // Seed a profile row (for dummy data). Auth is created by Register screen, not here.
        val defaultUserId = userDao.insert(User(name = "Default User", height = 0, weight = 0))

        val chestId = muscleGroupDao.insert(MuscleGroup(name = "Chest"))
        val legsId = muscleGroupDao.insert(MuscleGroup(name = "Legs"))
        val backId = muscleGroupDao.insert(MuscleGroup(name = "Back"))
        val shouldersId = muscleGroupDao.insert(MuscleGroup(name = "Shoulders"))
        val armsId = muscleGroupDao.insert(MuscleGroup(name = "Arms"))

        exerciseDao.insert(Exercises("Bench Press", 0, 0, true, chestId))
        exerciseDao.insert(Exercises("Squat", 0, 0, true, legsId))
        exerciseDao.insert(Exercises("Deadlift", 0, 0, false, backId))
        exerciseDao.insert(Exercises("Overhead Press", 0, 0, false, shouldersId))
        exerciseDao.insert(Exercises("Barbell Row", 0, 0, false, backId))
        exerciseDao.insert(Exercises("Pull-up", 0, 0, false, backId))
        exerciseDao.insert(Exercises("Bicep Curl", 0, 0, false, armsId))

        // Use the seeded profile id instead of hardcoded 1L
        seedDummyTemplates(userId = defaultUserId)
    }

    private suspend fun seedDummyTemplates(userId: Long) {
        val workoutTemplateDao = workoutTemplateDao()

        val existingCount = runCatching {
            workoutTemplateDao.countTemplatesByUserId(userId)
        }.getOrNull() ?: 0
        if (existingCount > 0) return

        val templateExerciseDao = templateExerciseDao()
        val templateSetDao = templateSetDao()
        val exerciseDao = exerciseDao()

        suspend fun exId(name: String): Long =
            exerciseDao.getExerciseIdByName(name)
                ?: error("Missing exercise '$name' - did seed exercises run?")

        suspend fun addExerciseWithSets(
            templateId: Long,
            exerciseName: String,
            sets: List<Pair<Float, Int>>
        ) {
            val templateExerciseId = templateExerciseDao.insert(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exId(exerciseName)
                )
            )

            sets.forEachIndexed { index, (weight, reps) ->
                templateSetDao.insert(
                    TemplateSet(
                        templateExerciseId = templateExerciseId,
                        setNumber = index + 1,
                        weight = weight,
                        reps = reps
                    )
                )
            }
        }

        val pushId = workoutTemplateDao.insert(
            WorkoutTemplate(date = "2026-01-07", name = "Push (Dummy)", userId = userId)
        )
        val pullId = workoutTemplateDao.insert(
            WorkoutTemplate(date = "2026-01-07", name = "Pull (Dummy)", userId = userId)
        )
        val legsId = workoutTemplateDao.insert(
            WorkoutTemplate(date = "2026-01-07", name = "Legs (Dummy)", userId = userId)
        )

        addExerciseWithSets(pushId, "Bench Press", listOf(60f to 10, 70f to 8, 75f to 6))
        addExerciseWithSets(pushId, "Overhead Press", listOf(30f to 10, 35f to 8, 40f to 6))
        addExerciseWithSets(pushId, "Bicep Curl", listOf(12f to 12, 14f to 10, 16f to 8))

        addExerciseWithSets(pullId, "Barbell Row", listOf(50f to 10, 60f to 8, 65f to 6))
        addExerciseWithSets(pullId, "Pull-up", listOf(0f to 8, 0f to 8, 0f to 6))

        addExerciseWithSets(legsId, "Squat", listOf(80f to 10, 90f to 8, 100f to 6))
    }
}
