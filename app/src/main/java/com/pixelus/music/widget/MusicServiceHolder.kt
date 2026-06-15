package com.pixelus.music.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.pixelus.music.player.MusicPlayer

object MusicServiceHolder {
    var context: Context? = null
    var player: MusicPlayer? = null

    private var prefs: SharedPreferences? = null

    fun init(ctx: Context, p: MusicPlayer) {
        context = ctx.applicationContext
        player = p
        prefs = context!!.getSharedPreferences("pixelus_widget", Context.MODE_PRIVATE)
    }

    fun publishState(
        title: String,
        artist: String,
        album: String,
        albumArtUri: String?,
        isPlaying: Boolean,
        positionMillis: Long = 0
    ) {
        prefs?.edit {
            putString("widget_title", title)
            putString("widget_artist", artist)
            putString("widget_album", album)
            putString("widget_album_art", albumArtUri)
            putBoolean("widget_playing", isPlaying)
            putLong("widget_position", positionMillis)
        }
    }

    fun getPrefs(ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences("pixelus_widget", Context.MODE_PRIVATE)
    }
}
