package com.realityexpander.guessasketch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

// Lottie Animation resources:
// https://lottiefiles.com/26941-global-network  -> Lottie Json
// https://freesvg.org/vector-image-of-a-cat-with-cute-smile - cute_cat svg
// https://lottiefiles.com/23662-laptop-animation-pink-navy-blue-white - pick_word

@HiltAndroidApp
class DrawingApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }
}