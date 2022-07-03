package com.realityexpander.guessasketch.util

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

// Prevents a Bug(?) when navigating to the same destination from the same navController
fun NavController.navigateSafely(
    @IdRes resId: Int,
    args: Bundle? = null,
    navOptions: NavOptions? = null,
    navExtras: Navigator.Extras? = null
){
    val action = currentDestination?.getAction(resId) ?: graph.getAction(resId)

    // If the destination is the same as the current destination, do nothing
    // This is a workaround for a bug where the same destination is navigated to twice
    // https://issuetracker.google.com/issues/153009076
    if(action != null && currentDestination?.id != action.destinationId) {
        // Only navigate if the destination is different than the current destination
        navigate(resId, args, navOptions, navExtras)
    }
}