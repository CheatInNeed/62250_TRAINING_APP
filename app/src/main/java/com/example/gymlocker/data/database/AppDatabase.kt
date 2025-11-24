package com.example.gymlocker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gymlocker.data.dao.*
import com.example.gymlocker.data.entity.*

@Database(
    entities = [
        User::class,
        Workout::class,
        MuscleGroup::class,
        Exercises::class,
        WorkoutExerciseCrossRef::class,
        ExerciseLog::class
    ],
    version = 2,                // <-- bump version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_locker_database"
                )
                    .fallbackToDestructiveMigration()   // wipe ved schema-ændringer (ok i dev)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Kald denne en enkelt gang ved app-start for at lægge nogle standardøvelser i DB.
         */
        suspend fun prepopulate(context: Context) {
            val db = getDatabase(context)
            val exerciseDao = db.exerciseDao()
            val muscleGroupDao = db.muscleGroupDao()

            if (exerciseDao.countExercises() > 0) return

            // Opret muskelgrupper
            val chestId = muscleGroupDao.insert(MuscleGroup(name = "Chest"))
            val legsId = muscleGroupDao.insert(MuscleGroup(name = "Legs"))
            val backId = muscleGroupDao.insert(MuscleGroup(name = "Back"))
            val shouldersId = muscleGroupDao.insert(MuscleGroup(name = "Shoulders"))
            val armsId = muscleGroupDao.insert(MuscleGroup(name = "Arms"))

            // Nogle standardøvelser – samme som dine hardcodede
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
}
