package com.example.gymlocker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gymlocker.data.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters


enum class StatsRange { WEEK, MONTH }

/**
 * AndroidViewModel is used so we can avoid a custom ViewModelFactory.
 * It provides an Application context via getApplication().
 *
 * Usage in Compose:
 *   val statViewModel: StatViewModel = viewModel()
 */
class StatViewModel(app: Application) : AndroidViewModel(app) {

    private val db by lazy { AppDatabase.getDatabase(app.applicationContext) }
    private val workoutDao by lazy { db.workoutDao() }
    private val performedSetDao by lazy { db.performedSetDao() }


    fun weeklyHoursLast3Months(userId: Long = 1L): Flow<List<WeekHoursUi>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        val today = LocalDate.now()
        val startDate = today.minusMonths(3)

        val startInclusive = startDate
            .atStartOfDay()
            .format(formatter)

        return workoutDao.observeWorkoutsFrom(userId = userId, startInclusive = startInclusive)
            .map { workouts ->
                val byWeek = mutableMapOf<LocalDate, Long>() // weekStart -> totalSeconds

                workouts.forEach { w ->
                    val ldt = runCatching { LocalDateTime.parse(w.date, formatter) }.getOrNull()
                        ?: return@forEach
                    val weekStart = ldt.toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    byWeek[weekStart] = (byWeek[weekStart] ?: 0L) + w.time
                }

                val firstWeek = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val lastWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeks = ChronoUnit.WEEKS.between(firstWeek, lastWeek).toInt().coerceAtLeast(0)

                (0..weeks).map { i ->
                    val ws = firstWeek.plusWeeks(i.toLong())
                    val seconds = byWeek[ws] ?: 0L
                    WeekHoursUi(
                        weekStart = ws,
                        hours = seconds / 3600f
                    )
                }
            }
    }

    fun weeklyVolumeLast3Months(userId: Long = 1L): Flow<List<WeekVolumeUi>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        val today = LocalDate.now()
        val startDate = today.minusMonths(3)

        val startInclusive = startDate
            .atStartOfDay()
            .format(formatter)

        return performedSetDao
            .observeWorkoutVolumesFrom(userId = userId, startInclusive = startInclusive)
            .map { rows ->
                val byWeek = mutableMapOf<LocalDate, Float>() // weekStart -> totalVolume

                rows.forEach { r ->
                    val ldt = runCatching { LocalDateTime.parse(r.date, formatter) }.getOrNull()
                        ?: return@forEach
                    val weekStart = ldt.toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    byWeek[weekStart] = (byWeek[weekStart] ?: 0f) + r.volume.toFloat()
                }

                val firstWeek = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val lastWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeks = ChronoUnit.WEEKS.between(firstWeek, lastWeek).toInt().coerceAtLeast(0)

                (0..weeks).map { i ->
                    val ws = firstWeek.plusWeeks(i.toLong())
                    WeekVolumeUi(
                        weekStart = ws,
                        volume = byWeek[ws] ?: 0f
                    )
                }
            }
    }

    // ----------------------------
    // Stats: range + distribution
    // ----------------------------

    private val _statsRange = MutableStateFlow(StatsRange.WEEK)
    val statsRange: StateFlow<StatsRange> = _statsRange

    fun setStatsRange(range: StatsRange) {
        _statsRange.value = range
    }

    fun muscleGroupDistribution(userId: Long = 1L): Flow<List<com.example.gymlocker.data.dao.MuscleGroupDistributionRow>> =
        _statsRange.flatMapLatest { range ->
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            val now = LocalDateTime.now()

            val start = when (range) {
                StatsRange.WEEK -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate().atStartOfDay()

                StatsRange.MONTH -> now.withDayOfMonth(1).toLocalDate().atStartOfDay()
            }

            val startInclusive = start.format(fmt)
            val endExclusive = now.plusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0)
                .format(fmt)

            performedSetDao.observeMuscleGroupDistribution(
                userId = userId,
                startInclusive = startInclusive,
                endExclusive = endExclusive
            )
        }
}
