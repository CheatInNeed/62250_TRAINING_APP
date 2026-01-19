package com.example.gymlocker.data.import

import android.content.Context
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.MuscleGroup
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

object ExerciseCsvImporter {

    suspend fun importFromRawResource(
        context: Context,
        db: AppDatabase,
        rawResId: Int
    ) {
        context.resources.openRawResource(rawResId).use { input ->
            importFromStream(db, input)
        }
    }

    suspend fun importFromStream(
        db: AppDatabase,
        input: InputStream
    ) {
        val exerciseDao = db.exerciseDao()
        val muscleGroupDao = db.muscleGroupDao()

        // Cache eksisterende muskelgrupper (for at undgå duplicates)
        val mgCache: MutableMap<String, Long> =
            muscleGroupDao.getAllOnce()
                .associate { it.name.trim().lowercase(Locale.ROOT) to it.muscleGroupId }
                .toMutableMap()

        BufferedReader(InputStreamReader(input)).use { reader ->
            val lines = reader.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .toList()

            if (lines.isEmpty()) return

            val delimiter = detectDelimiter(lines.first())
            val header = splitCsvLine(lines.first(), delimiter).map { it.lowercase(Locale.ROOT) }

            fun idx(col: String): Int = header.indexOf(col).also {
                require(it >= 0) { "CSV mangler kolonnen '$col'. Header=$header" }
            }

            val iName = idx("name")
            val iMg = idx("muscle_group")
            val iStartWeight = header.indexOf("start_weight")
            val iStartReps = header.indexOf("start_reps")
            val iRecent = header.indexOf("is_recent")

            // Data rows
            for (rowLine in lines.drop(1)) {
                val cols = splitCsvLine(rowLine, delimiter)

                val name = cols.getOrNull(iName)?.trim().orEmpty()
                val mgName = cols.getOrNull(iMg)?.trim().orEmpty()
                if (name.isBlank() || mgName.isBlank()) continue

                val mgKey = mgName.lowercase(Locale.ROOT)
                val mgId = mgCache[mgKey] ?: run {
                    // Hvis mg ikke findes, opret den
                    val newId = muscleGroupDao.insert(MuscleGroup(name = mgName))
                    mgCache[mgKey] = newId
                    newId
                }

                val startWeight = cols.getOrNull(iStartWeight)?.trim()?.toIntOrNull() ?: 0
                val startReps = cols.getOrNull(iStartReps)?.trim()?.toIntOrNull() ?: 0
                val isRecent = cols.getOrNull(iRecent)?.trim()?.let(::parseBool) ?: false

                // Undgå crash på duplicate exercise name (I har exists-check allerede)
                val exists = exerciseDao.existsByNameIgnoreCase(name)
                if (exists) continue

                exerciseDao.insert(
                    Exercises(
                        name = name,
                        startWeight = startWeight,
                        startReps = startReps,
                        isRecent = isRecent,
                        muscleGroupId = mgId
                    )
                )
            }
        }
    }

    private fun parseBool(v: String): Boolean {
        return when (v.trim().lowercase(Locale.ROOT)) {
            "1", "true", "yes", "y" -> true
            "0", "false", "no", "n" -> false
            else -> false
        }
    }

    private fun detectDelimiter(headerLine: String): Char {
        // Prioritér ; (som vi bruger), ellers ,
        return if (headerLine.contains(';')) ';' else ','
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        // Simpel splitter (til jeres brug). Hvis I senere vil have fuld CSV quoting-support,
        // kan vi udvide den.
        return line.split(delimiter).map { it.trim().trim('"') }
    }
}
