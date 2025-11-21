package com.example.gymlocker.di

import android.content.Context
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.repository.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context) = AppDatabase.getDatabase(context)

    @Singleton
    @Provides
    fun provideWorkoutRepository(appDatabase: AppDatabase) = WorkoutRepository(
        workoutDao = appDatabase.workoutDao(),
        exerciseDao = appDatabase.exerciseDao(),
        setsDao = appDatabase.setsDao(),
        workoutExerciseCrossRefDao = appDatabase.workoutExerciseCrossRefDao()
    )
}
