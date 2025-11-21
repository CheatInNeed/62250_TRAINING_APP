package com.example.gymlocker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gymlocker.data.dao.*
import com.example.gymlocker.data.entity.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

@Database(entities = [User::class, Workout::class, MuscleGroup::class, Exercises::class, WorkoutExerciseCrossRef::class, ExerciseLog::class, Sets::class],
          version = 1,
          exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun workoutExerciseCrossRefDao(): WorkoutExerciseCrossRefDao
    abstract fun setsDao(): SetsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_locker_database"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Executors.newSingleThreadExecutor().execute {
                            runBlocking {
                                getDatabase(context).let { database ->
                                    val muscleGroupDao = database.muscleGroupDao()
                                    val exerciseDao = database.exerciseDao()

                                    val muscleGroupId = muscleGroupDao.insert(MuscleGroup(name = "General"))

                                    val exercises = listOf(
                                        "Arm Curl", "Back Extension", "Ball Slam", "Bench Press", "Burpee", "Crunch",
                                        "Deadlift", "Double Arm Triceps Extension", "Dumbbell Row", "Front Raise",
                                        "Hip Thrust", "Hula Hoop", "Jumping Jack", "Jump Rope", "Kettlebell Swing",
                                        "Lateral Raise", "Lat Pull Down", "Leg Curl", "Leg Extension", "Leg Press",
                                        "Leg Raise", "Lunge", "Mountain Climber", "Plank", "Pull Up", "Punch",
                                        "Shoulder Press", "Single Arm Triceps Extension", "Sit Up", "Squat"
                                    ).map {
                                        Exercises(name = it, startWeight = 0, startReps = 0, isRecent = false, muscleGroupId = muscleGroupId)
                                    }
                                    exerciseDao.insertAll(exercises)
                                }
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
