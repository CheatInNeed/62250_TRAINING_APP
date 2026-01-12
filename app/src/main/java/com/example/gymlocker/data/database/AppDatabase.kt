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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Database(
    entities = [
        User::class,
        AuthAccount::class,

        Workout::class,
        MuscleGroup::class,
        Exercises::class,
        ExerciseLog::class,
        PerformedSet::class,

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
                    database.seed10WorkoutsForGraphTesting(database, userId = 1L)
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

    /*
     * Seeds 10 completed workouts spread across many weeks and muscle groups,
     * so WeeklyHoursChart + Training balance chart have data to show.
     *
     * Assumptions:
     * - Workouts have `time: Long` (seconds)
     * - You already have MuscleGroups + Exercises prepopulated
     * - exercise_log + performed_set are used for completion / muscle distribution
     */
    suspend fun seed10WorkoutsForGraphTesting(
        db: AppDatabase,
        userId: Long = 1L
    ) {
        val workoutDao = db.workoutDao()
        val exerciseDao = db.exerciseDao()
        val muscleGroupDao = db.muscleGroupDao()
        val exerciseLogDao = db.exerciseLogDao()
        val performedSetDao = db.performedSetDao()

        // Safety: require exercises + muscle groups
        val exercises = exerciseDao.getAllOnce()
        val muscleGroups = muscleGroupDao.getAllOnce()

        if (exercises.isEmpty() || muscleGroups.isEmpty()) return

        // Group exercises by muscle group so we can pick diverse ones
        val exercisesByMg: Map<Long, List<Long>> =
            exercises.groupBy { it.muscleGroupId }.mapValues { (_, exs) -> exs.map { it.exerciseId } }

        // Pick muscle groups that actually have exercises
        val usableMgIds = muscleGroups
            .map { it.muscleGroupId }
            .filter { (exercisesByMg[it]?.isNotEmpty() == true) }

        if (usableMgIds.size < 3) return // need at least 3 groups for “spread”

        // If you want to avoid duplicating on every app start, you can add a guard here.
        // Example: bail out if there are already >=10 workouts.
        // (Add this DAO helper if you want it: @Query("SELECT COUNT(*) FROM workouts WHERE userId=:userId") suspend fun countWorkouts(userId: Long): Int)
        // if (workoutDao.countWorkouts(userId) >= 10) return

        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        // Dates spread across ~3 months (roughly 11 weeks), not all same week
        val now = LocalDateTime.now()
        val workoutDates = listOf(
            now.minusDays(2),
            now.minusDays(8),
            now.minusDays(15),
            now.minusDays(22),
            now.minusDays(29),
            now.minusDays(36),
            now.minusDays(43),
            now.minusDays(50),
            now.minusDays(57),
            now.minusDays(71)
        )

        // Rotate through muscle groups so distribution chart is varied
        // We'll pick 3 different groups per workout
        fun pick3DistinctMuscleGroups(seed: Int): List<Long> {
            val shuffled = usableMgIds.shuffled(Random(seed))
            return shuffled.take(3)
        }

        // Helper to pick one exercise id from a muscle group
        fun pickExerciseId(mgId: Long, seed: Int): Long {
            val ids = exercisesByMg[mgId] ?: emptyList()
            return ids[seed % ids.size]
        }

        workoutDates.forEachIndexed { i, dt ->
            val dateString = dt.format(fmt)

            // workout time in seconds (30–95 minutes)
            val timeSeconds = listOf(1800L, 2400L, 2700L, 3300L, 3600L, 4200L, 4800L, 5400L, 5700L).random(Random(1000 + i))

            val workoutId = workoutDao.insert(
                Workout(
                    date = dateString,
                    name = "Seed Workout ${i + 1}",
                    userId = userId,
                    time = timeSeconds
                )
            )

            val mgIds = pick3DistinctMuscleGroups(seed = 2000 + i)

            mgIds.forEachIndexed { j, mgId ->
                val exerciseId = pickExerciseId(mgId, seed = 3000 + i * 10 + j)

                // ensure exercise_log row exists
                val logId = exerciseLogDao.getOrCreateLogId(workoutId, exerciseId)

                // 3 completed sets with semi-realistic weights/reps
                // Keep it varied so PR + “last set” features still look plausible.
                val baseReps = listOf(6, 8, 10, 12).random(Random(4000 + i * 10 + j))
                val baseWeight = listOf(20f, 30f, 40f, 50f, 60f).random(Random(5000 + i * 10 + j))

                (1..3).forEach { setNo ->
                    val reps = (baseReps - (setNo - 1)).coerceAtLeast(4)
                    val weight = baseWeight + (setNo - 1) * 2.5f

                    performedSetDao.insert(
                        PerformedSet(
                            exerciseLogId = logId,
                            setNumber = setNo,
                            weight = weight,
                            reps = reps,
                            isCompleted = true
                        )
                    )
                }
            }
        }
    }

}
