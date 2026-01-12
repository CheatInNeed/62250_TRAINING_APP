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
        AuthProfile::class,

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
    abstract fun authProfileDao(): AuthProfileDao

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

        /**
         * ✅ ONLY when true:
         * - wipe database + session
         * - seed test account/user/profile
         * - seed muscle groups + exercises
         * - seed workout data for graphs
         *
         * ❌ when false: NONE of the above happens.
         */
        private const val DEBUG_WIPE_DB = true

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                if (DEBUG_WIPE_DB) {
                    wipeDatabaseAndSession(context)
                }

                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )

                // Strongly recommended: only destructive migration in debug.
                if (DEBUG_WIPE_DB) {
                    builder.fallbackToDestructiveMigration()
                    builder.addCallback(DebugSeedCallback())
                } else {
                    // In production you should provide real migrations.
                    // Intentionally NO callback and NO wipe.
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        private fun wipeDatabaseAndSession(context: Context) {
            context.deleteDatabase(DB_NAME)

            val dbFile = context.getDatabasePath(DB_NAME)
            File(dbFile.path + "-shm").delete()
            File(dbFile.path + "-wal").delete()

            // adjust if your session file name/path differs
            val sessionFile = File(context.filesDir, "datastore/session.preferences_pb")
            if (sessionFile.exists()) sessionFile.delete()
        }
    }

    /**
     * Only registered when DEBUG_WIPE_DB = true.
     */
    private class DebugSeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val database = INSTANCE ?: return
            CoroutineScope(Dispatchers.IO).launch {
                database.debugSeedEverything()
            }
        }
    }

    /**
     * Runs ONLY in debug wipe mode.
     * Creates:
     * - test auth account
     * - test user (forced id=1)
     * - auth profile link
     * - muscle groups + exercises
     * - 10 workouts across many weeks with performed sets
     */
    private suspend fun debugSeedEverything() {
        // Extra safety: if someone accidentally calls this, do nothing unless toggle is true.
        if (!DEBUG_WIPE_DB) return

        seedTestLoginAndProfile()
        seedMuscleGroupsAndExercisesIfEmpty()
        seed10WorkoutsForGraphTesting(userId = 1L)
    }

    private suspend fun seedTestLoginAndProfile() {
        val authDao = authAccountDao()
        val userDao = userDao()
        val profileDao = authProfileDao()

        // TODO remove before launch (but this function only runs with DEBUG_WIPE_DB=true)
        val email = "test@test.dk"
        val password = "password"
        val normalizedEmail = email.trim().lowercase()

        val existingAccount = authDao.findByEmail(normalizedEmail)
        val authId = existingAccount?.authId ?: authDao.insert(
            AuthAccount(
                email = normalizedEmail,
                passwordHash = com.example.gymlocker.data.auth.PasswordHasher.sha256(password)
            )
        )

        val seededUserId = 1L
        val existingUser = userDao.getUserOnce(seededUserId)
        if (existingUser == null) {
            userDao.insert(
                User(
                    userId = seededUserId,
                    name = "Test",
                    height = 180,
                    weight = 80
                )
            )
        }

        // Link auth -> profile.
        // If your DAO uses OnConflict=ABORT, this might throw on re-run; we guard it.
        runCatching {
            profileDao.insert(AuthProfile(authId = authId, userId = seededUserId))
        }
    }

    private suspend fun seedMuscleGroupsAndExercisesIfEmpty() {
        val exerciseDao = exerciseDao()
        val muscleGroupDao = muscleGroupDao()

        val exercisesCount = runCatching { exerciseDao.countExercises() }.getOrNull() ?: 0
        if (exercisesCount > 0) return

        val chestId = muscleGroupDao.insert(MuscleGroup(name = "Chest"))
        val legsId = muscleGroupDao.insert(MuscleGroup(name = "Legs"))
        val backId = muscleGroupDao.insert(MuscleGroup(name = "Back"))
        val shouldersId = muscleGroupDao.insert(MuscleGroup(name = "Shoulders"))
        val armsId = muscleGroupDao.insert(MuscleGroup(name = "Arms"))

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

    /**
     * Seeds 10 completed workouts spread across many weeks and muscle groups,
     * so WeeklyHoursChart + Training balance chart have data.
     *
     * Assumptions:
     * - Workouts have `time: Long` (seconds)
     * - exercise_log + performed_set are used for completion / muscle distribution
     * - ExerciseLogDao has: suspend fun getOrCreateLogId(workoutId: Long, exerciseId: Long): Long
     */
    private suspend fun seed10WorkoutsForGraphTesting(userId: Long = 1L) {
        val workoutDao = workoutDao()
        val exerciseDao = exerciseDao()
        val muscleGroupDao = muscleGroupDao()
        val exerciseLogDao = exerciseLogDao()
        val performedSetDao = performedSetDao()

        val exercises = exerciseDao.getAllOnce()
        val muscleGroups = muscleGroupDao.getAllOnce()
        if (exercises.isEmpty() || muscleGroups.isEmpty()) return

        val exercisesByMg: Map<Long, List<Long>> =
            exercises.groupBy { it.muscleGroupId }.mapValues { (_, exs) -> exs.map { it.exerciseId } }

        val usableMgIds = muscleGroups
            .map { it.muscleGroupId }
            .filter { (exercisesByMg[it]?.isNotEmpty() == true) }

        if (usableMgIds.size < 3) return

        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val now = LocalDateTime.now()

        val workoutDates = listOf(
            now.minusDays(0),
            now.minusDays(2),
            now.minusDays(8),
            now.minusDays(15),
            now.minusDays(22),
            now.minusDays(29),
            now.minusDays(36),
            now.minusDays(43),
            now.minusDays(50),
            now.minusDays(57),
        ).reversed()

        fun pick3DistinctMuscleGroups(seed: Int): List<Long> =
            usableMgIds.shuffled(Random(seed)).take(3)

        fun pickExerciseId(mgId: Long, seed: Int): Long {
            val ids = exercisesByMg[mgId] ?: emptyList()
            return ids[seed % ids.size]
        }

        workoutDates.forEachIndexed { i, dt ->
            val dateString = dt.format(fmt)

            val timeSeconds = listOf(
                1800L, 2400L, 2700L, 3300L, 3600L, 4200L, 4800L, 5400L, 5700L
            ).random(Random(1000 + i))

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

                val logId = exerciseLogDao.getOrCreateLogId(workoutId, exerciseId)

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
