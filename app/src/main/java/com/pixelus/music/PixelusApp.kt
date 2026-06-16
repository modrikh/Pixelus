package com.pixelus.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.pixelus.music.data.MusicScanner
import com.pixelus.music.data.PixelusSettings
import com.pixelus.music.data.PlayerStateManager
import com.pixelus.music.data.playlist.PlaylistRepository
import com.pixelus.music.data.metadata.MetadataWriterImpl
import com.pixelus.music.player.MusicService
import com.pixelus.music.widget.MusicServiceHolder

class PixelusApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = PixelusSettings(this)
        playlistRepository = PlaylistRepository(this)
        metadataWriter = MetadataWriterImpl(this)
        playerStateManager = PlayerStateManager(this)
        createNotificationChannel()
        MusicService.initPlayer(this)
        MusicServiceHolder.init(this, MusicService.player)
        musicScanner = MusicScanner(this, settings)
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
        lateinit var instance: PixelusApp
            private set
        lateinit var settings: PixelusSettings
            private set
        lateinit var musicScanner: MusicScanner
            private set
        lateinit var playlistRepository: PlaylistRepository
            private set
        lateinit var metadataWriter: MetadataWriterImpl
            private set
        lateinit var playerStateManager: PlayerStateManager
            private set
    }
}
