package com.example.gymlocker.ui.util

import androidx.navigation.NavController

/**
 * Pops the back stack only if there is something to pop.
 * (Prevents "empty screen" when spam-tapping back at root.)
 */
fun NavController.popBackUnlessAtRoot(): Boolean {
    val canPop = previousBackStackEntry != null
    return if (canPop) popBackStack() else false
}
