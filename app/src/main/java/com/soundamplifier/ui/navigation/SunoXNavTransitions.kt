package com.soundamplifier.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

private val FadeMs = tween<Float>(durationMillis = 280)

fun sunoxEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(FadeMs) + slideInHorizontally { fullWidth -> fullWidth / 5 }
}

fun sunoxExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(FadeMs) + slideOutHorizontally { fullWidth -> -fullWidth / 5 }
}

fun sunoxPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(FadeMs) + slideInHorizontally { fullWidth -> -fullWidth / 5 }
}

fun sunoxPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(FadeMs) + slideOutHorizontally { fullWidth -> fullWidth / 5 }
}
