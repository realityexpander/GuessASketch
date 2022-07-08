package com.realityexpander.guessasketch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

// Lottie Animation resources:
// https://lottiefiles.com/26941-global-network
// https://freesvg.org/vector-image-of-a-cat-with-cute-smile
//

@HiltAndroidApp
class DrawingApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }
}