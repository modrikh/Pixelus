package com.pixelus.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.pixelus.music.player.MusicService
import com.pixelus.music.widget.MusicServiceHolder

class PixelusApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        MusicService.initPlayer(this)
        MusicServiceHolder.init(this, MusicService.player)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "music_playback"
    }
}
