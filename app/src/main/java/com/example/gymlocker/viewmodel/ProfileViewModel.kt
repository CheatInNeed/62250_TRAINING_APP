package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.AuthProfile
import com.example.gymlocker.data.entity.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileWorkoutSummaryUi(
    val totalWorkouts: Int = 0,
    val mostRecentName: String? = null,
    val mostRecentDate: String? = null
)

class ProfileViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val session by lazy { SessionManager(appContext) }

    private val userDao by lazy { db.userDao() }
    private val authProfileDao by lazy { db.authProfileDao() }
    private val workoutDao by lazy { db.workoutDao() }

    val authId: StateFlow<Long?> = session.authId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeProfileUserId: StateFlow<Long?> = session.activeProfileUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All profiles owned by this auth account
    val profiles: StateFlow<List<User>> =
        authId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else userDao.observeProfilesForAuth(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active profile object (or null)
    val activeProfile: StateFlow<User?> =
        activeProfileUserId.flatMapLatest { userId ->
            if (userId == null) flowOf(null)
            else userDao.getUser(userId).map { it as User? }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Workout summary for active profile
    val workoutSummary: StateFlow<ProfileWorkoutSummaryUi> =
        activeProfileUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(ProfileWorkoutSummaryUi())
            } else {
                workoutDao.getWorkoutSummariesForUser(userId).map { list ->
                    if (list.isEmpty()) {
                        ProfileWorkoutSummaryUi(totalWorkouts = 0, mostRecentName = null, mostRecentDate = null)
                    } else {
                        val mostRecent = list.first()
                        ProfileWorkoutSummaryUi(
                            totalWorkouts = list.size,
                            mostRecentName = mostRecent.name,
                            mostRecentDate = mostRecent.date
                        )
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileWorkoutSummaryUi())

    fun setActiveProfile(userId: Long) {
        viewModelScope.launch {
            session.setActiveProfile(userId)
        }
    }

    fun clearActiveProfile() {
        viewModelScope.launch {
            session.clearActiveProfile()
        }
    }

    fun createProfile(name: String, height: Int, weight: Int, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val aId = authId.value ?: return@launch

            val newUserId = userDao.insert(
                User(
                    name = name.trim(),
                    height = height,
                    weight = weight
                )
            )

            authProfileDao.insert(AuthProfile(authId = aId, userId = newUserId))
            session.setActiveProfile(newUserId)

            onDone?.invoke()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(appContext) as T
                }
            }
        }
    }
}
