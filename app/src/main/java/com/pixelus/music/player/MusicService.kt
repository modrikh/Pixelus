package com.pixelus.music.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pixelus.music.MainActivity

@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        player.exoPlayer.addListener(object : Player.Listener {
            @OptIn(UnstableApi::class)
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    val sessionId = player.exoPlayer.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        equalizerController.updateEqualizer(sessionId)
                    }
                }
            }
        })

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player.exoPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        equalizerController.releaseEqualizer()
        mediaSession?.run {
            release()
        }
        super.onDestroy()
    }

    companion object {
        lateinit var player: MusicPlayer
            private set
        lateinit var equalizerController: EqualizerController
            private set

        fun initPlayer(context: android.content.Context) {
            player = MusicPlayer(context.applicationContext)
            equalizerController = EqualizerController(context.applicationContext)
        }
    }
}
