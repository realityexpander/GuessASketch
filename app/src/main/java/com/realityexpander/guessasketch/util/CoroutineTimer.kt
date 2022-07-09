package com.realityexpander.guessasketch.util

import android.hardware.ConsumerIrManager.CarrierFrequencyRange
import kotlinx.coroutines.*

class CoroutineCountdownTimer {

    // Count down timer for the game
    fun timeAndEmitJob(
        durationMillis: Long,   // Length of time for this timer to run
        coroutineScope: CoroutineScope,
        emissionFrequencyMillis: Long = 100L,  // How often to emit an update
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        onTick: (Long) -> Unit,
    ): Job {
        return coroutineScope.launch(dispatcher) {
            var time = durationMillis
            while (time >= 0) {
                onTick(time)
                delay(emissionFrequencyMillis)
                time -= emissionFrequencyMillis
            }
        }
    }
}