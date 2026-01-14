package com.example.gymlocker.util

import com.example.gymlocker.data.entity.WeightUnit
import kotlin.math.roundToInt

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
    val factor = Math.pow(10.0, decimals.toDouble())
    val rounded = (weight * factor).roundToInt() / factor
    return rounded.toString()
}
