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
        AuthAccount::class,

        Workout::class,
        MuscleGroup::class,
        Exercises::class,
        ExerciseLog::class,
        PerformedSet::class,

        ExerciseRestPreference::class,

        WorkoutTemplate::class,
        TemplateExercise::class,
        TemplateSet::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
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

    abstract fun exerciseRestPreferenceDao(): ExerciseRestPreferenceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "gymlocker.db"

        // 🔥 Toggle this when debugging
        private const val DEBUG_WIPE_DB = false

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                if (DEBUG_WIPE_DB) {
                    wipeDatabaseAndSession(context)
                }

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

        private fun wipeDatabaseAndSession(context: Context) {
            // --- Room DB ---
            context.deleteDatabase(DB_NAME)

            val dbFile = context.getDatabasePath(DB_NAME)
            File(dbFile.path + "-shm").delete()
            File(dbFile.path + "-wal").delete()

            // --- DataStore session ---
            // preferencesDataStore(name = "session")
            val sessionFile = File(context.filesDir, "datastore/session.preferences_pb")
            if (sessionFile.exists()) sessionFile.delete()
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

        val usersCount = runCatching { userDao.countUsers() }.getOrNull() ?: 0
        val exercisesCount = runCatching { exerciseDao.countExercises() }.getOrNull() ?: 0

        if (usersCount > 0 || exercisesCount > 0) return

        // Seed a profile row (dummy)
        val defaultUserId = userDao.insert(
            User(
                name = "Default User",
                height = 0,
                weight = 0
            )
        )

        // Seed muscle groups
        val chestId = muscleGroupDao.insert(MuscleGroup(name = "Chest"))
        val legsId = muscleGroupDao.insert(MuscleGroup(name = "Legs"))
        val backId = muscleGroupDao.insert(MuscleGroup(name = "Back"))
        val shouldersId = muscleGroupDao.insert(MuscleGroup(name = "Shoulders"))
        val armsId = muscleGroupDao.insert(MuscleGroup(name = "Arms"))

        // ✅ IMPORTANT: use named params so types match your entity
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
            // ✅ named params so it matches your TemplateExercise entity (likely Long, Long)
            val templateExerciseId = templateExerciseDao.insert(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exId(exerciseName)
                )
            )

            sets.forEachIndexed { index, (weight, reps) ->
                // ✅ named params to match your TemplateSet entity
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

        // ✅ named params so date/name stay String and userId stays Long
        val pushId = workoutTemplateDao.insert(
            WorkoutTemplate(
                date = "2026-01-07",
                name = "Push (Dummy)",
                userId = userId
            )
        )
        val pullId = workoutTemplateDao.insert(
            WorkoutTemplate(
                date = "2026-01-07",
                name = "Pull (Dummy)",
                userId = userId
            )
        )
        val legsId = workoutTemplateDao.insert(
            WorkoutTemplate(
                date = "2026-01-07",
                name = "Legs (Dummy)",
                userId = userId
            )
        )

        addExerciseWithSets(pushId, "Bench Press", listOf(60f to 10, 70f to 8, 75f to 6))
        addExerciseWithSets(pushId, "Overhead Press", listOf(30f to 10, 35f to 8, 40f to 6))
        addExerciseWithSets(pushId, "Bicep Curl", listOf(12f to 12, 14f to 10, 16f to 8))

        addExerciseWithSets(pullId, "Barbell Row", listOf(50f to 10, 60f to 8, 65f to 6))
        addExerciseWithSets(pullId, "Pull-up", listOf(0f to 8, 0f to 8, 0f to 6))

        addExerciseWithSets(legsId, "Squat", listOf(80f to 10, 90f to 8, 100f to 6))
    }
}
