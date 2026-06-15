package com.pixelus.music.player

import kotlinx.coroutines.*

class SleepTimer {

    private var job: Job? = null
    var remainingMs: Long = 0
        private set
    var isActive: Boolean = false
        private set

    fun start(durationMs: Long, onFinish: () -> Unit) {
        stop()
        isActive = true
        remainingMs = durationMs
        job = CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis()
            while (this@SleepTimer.isActive) {
                remainingMs = durationMs - (System.currentTimeMillis() - startTime)
                if (remainingMs <= 0) {
                    this@SleepTimer.isActive = false
                    remainingMs = 0
                    onFinish()
                    break
                }
                delay(1000)
            }
        }
    }

    fun stop() {
        isActive = false
        job?.cancel()
        job = null
        remainingMs = 0
    }

    fun getFormattedRemaining(): String {
        if (!isActive || remainingMs <= 0) return "Off"
        val totalSec = remainingMs / 1000
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
}
