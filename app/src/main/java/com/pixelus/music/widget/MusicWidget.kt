package com.pixelus.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.pixelus.music.MainActivity
import com.pixelus.music.R


class MusicWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_PLAYBACK -> {
                MusicWidgetRemote.togglePlayback(context)
            }
            ACTION_NEXT -> {
                MusicWidgetRemote.next(context)
            }
            ACTION_PREVIOUS -> {
                MusicWidgetRemote.previous(context)
            }
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MusicWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val options: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)

        val isXsmall = width < 100
        val layoutId = when {
            width >= 300 -> R.layout.music_widget_large
            width >= 200 -> R.layout.music_widget_medium
            width >= 100 -> R.layout.music_widget_small
            else -> R.layout.music_widget_xsmall
        }

        val views = RemoteViews(context.packageName, layoutId)
        val isPlaying = MusicWidgetRemote.isPlaying(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchPending = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(context, MusicWidget::class.java).apply {
            action = ACTION_TOGGLE_PLAYBACK
        }
        val playPausePending = PendingIntent.getBroadcast(
            context, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (isXsmall) {
            views.setOnClickPendingIntent(R.id.widget_album_art, playPausePending)
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePending)
        } else {
            views.setOnClickPendingIntent(R.id.widget_album_art, launchPending)
            views.setOnClickPendingIntent(R.id.widget_song_title, launchPending)
            views.setOnClickPendingIntent(R.id.widget_artist, launchPending)
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePending)
        }

        if (!isXsmall) {
            val nextIntent = Intent(context, MusicWidget::class.java).apply {
                action = ACTION_NEXT
            }
            views.setOnClickPendingIntent(
                R.id.widget_next,
                PendingIntent.getBroadcast(
                    context, 2, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val prevIntent = Intent(context, MusicWidget::class.java).apply {
                action = ACTION_PREVIOUS
            }
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                PendingIntent.getBroadcast(
                    context, 3, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val albumArt = MusicWidgetRemote.getAlbumArtBitmap(context)
        if (albumArt != null) {
            views.setImageViewBitmap(R.id.widget_album_art, albumArt)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_music_note)
        }

        if (isPlaying && isXsmall) {
            val rotation = (MusicWidgetRemote.getPositionMillis(context) / 10) % 360
            views.setFloat(R.id.widget_album_art, "setRotation", rotation.toFloat())
        }

        if (!isXsmall) {
            safeSetText(views, R.id.widget_song_title, MusicWidgetRemote.getTitle(context))
            safeSetText(views, R.id.widget_artist, MusicWidgetRemote.getArtist(context))
            safeSetText(views, R.id.widget_album, MusicWidgetRemote.getAlbum(context))
        }

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun safeSetText(views: RemoteViews, id: Int, text: String) {
        try {
            views.setTextViewText(id, text)
        } catch (_: Exception) { }
    }

    companion object {
        const val ACTION_TOGGLE_PLAYBACK = "com.pixelus.music.TOGGLE_PLAYBACK"
        const val ACTION_NEXT = "com.pixelus.music.NEXT"
        const val ACTION_PREVIOUS = "com.pixelus.music.PREVIOUS"
    }
}

object MusicWidgetRemote {

    private fun prefs(context: Context) = MusicServiceHolder.getPrefs(context)

    fun getTitle(context: Context): String =
        prefs(context).getString("widget_title", "No music playing") ?: "No music playing"

    fun getArtist(context: Context): String =
        prefs(context).getString("widget_artist", "") ?: ""

    fun getAlbum(context: Context): String =
        prefs(context).getString("widget_album", "") ?: ""

    fun isPlaying(context: Context): Boolean =
        prefs(context).getBoolean("widget_playing", false)

    fun getPositionMillis(context: Context): Long =
        prefs(context).getLong("widget_position", 0)

    fun getAlbumArtBitmap(context: Context): Bitmap? {
        val path = prefs(context).getString("widget_album_art", null) ?: return null
        return try {
            val uri = Uri.parse(path)
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun togglePlayback(context: Context) {
        MusicServiceHolder.player?.togglePlayPause()
    }

    fun next(context: Context) {
        MusicServiceHolder.player?.skipToNext()
    }

    fun previous(context: Context) {
        MusicServiceHolder.player?.skipToPrevious()
    }
}
