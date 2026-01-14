package com.example.gymlocker.util

import com.example.gymlocker.data.entity.WeightUnit
import kotlin.math.roundToInt
import java.util.Locale
import kotlin.math.pow

private const val KG_TO_LB = 2.2046226218

fun kgToLb(kg: Double): Double = kg * KG_TO_LB
fun lbToKg(lb: Double): Double = lb / KG_TO_LB

/**
 * Converts a value stored in KG (your database canonical unit)
 * into the unit the user wants to display.
 */
fun displayWeightFromKg(
    weightKg: Double,
    unit: WeightUnit
): Double {
    return when (unit) {
        WeightUnit.KG -> weightKg
        WeightUnit.LB -> kgToLb(weightKg)
    }
}

/**
 * Converts a weight the user typed (in their preferred unit)
 * into KG for storage in DB.
 */
fun storageKgFromInput(
    inputWeight: Double,
    unit: WeightUnit
): Double {
    return when (unit) {
        WeightUnit.KG -> inputWeight
        WeightUnit.LB -> lbToKg(inputWeight)
    }
}

fun weightUnitLabel(unit: WeightUnit): String =
    when (unit) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }

/**
 * Optional nice formatting helper.
 */
fun formatWeight(
    weight: Double,
    decimals: Int = 1
): String {
    // When decimals == 0 we want "80" not "80.0"
    if (decimals <= 0) return weight.roundToInt().toString()

    val factor = 10.0.pow(decimals)
    val rounded = (weight * factor).roundToInt() / factor

    // Keep a stable decimal format (e.g. 80.5, 80.25)
    return String.format(Locale.US, "%.${decimals}f", rounded)
}