package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val workoutDao by lazy { db.workoutDao() }

    val authId: StateFlow<Long?> = session.authId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeProfileUserId: StateFlow<Long?> = session.activeProfileUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<User>> =
        authId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else userDao.observeProfilesForAuth(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<User?> =
        activeProfileUserId.flatMapLatest { userId ->
            if (userId == null) flowOf(null)
            else userDao.getUser(userId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Requires WorkoutDao.getWorkoutSummariesForUser(userId).
     * If your build complains here, tell me and I’ll paste the exact DAO query.
     */
    val workoutSummary: StateFlow<ProfileWorkoutSummaryUi> =
        activeProfileUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(ProfileWorkoutSummaryUi())
            } else {
                workoutDao.getWorkoutSummariesForUser(userId).map { list ->
                    if (list.isEmpty()) {
                        ProfileWorkoutSummaryUi()
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
        viewModelScope.launch { session.setActiveProfile(userId) }
    }

    fun clearActiveProfile() {
        viewModelScope.launch { session.clearActiveProfile() }
    }

    fun createProfile(
        name: String,
        height: Int,
        weight: Int,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val aId = authId.value ?: return@launch

            val newUserId = userDao.insert(
                User(
                    authOwnerId = aId,
                    name = name.trim(),
                    height = height,
                    weight = weight
                )
            )

            session.setActiveProfile(newUserId)
            onDone?.invoke()
        }
    }

    // -----------------------------------------
    // ✅ Edit + validation + reset
    // -----------------------------------------

    fun saveProfileEdits(
        name: String,
        height: Int?,
        weight: Int?,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val userId = activeProfileUserId.value ?: return@launch

            val cleanName = name.trim()
            if (cleanName.isBlank()) {
                onError("Name cannot be empty.")
                return@launch
            }

            val h = height ?: 0
            val w = weight ?: 0

            // allow 0 = Not set
            if (h != 0 && (h < 50 || h > 250)) {
                onError("Height must be 50–250 cm, or leave it empty.")
                return@launch
            }
            if (w != 0 && (w < 20 || w > 300)) {
                onError("Weight must be 20–300 kg, or leave it empty.")
                return@launch
            }

            userDao.updateBasics(
                userId = userId,
                name = cleanName,
                height = h,
                weight = w
            )

            onSuccess()
        }
    }

    fun resetActiveProfile(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val userId = activeProfileUserId.value ?: return@launch
            userDao.resetBasics(userId)
            onSuccess()
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
