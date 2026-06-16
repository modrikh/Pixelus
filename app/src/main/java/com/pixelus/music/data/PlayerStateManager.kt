package com.pixelus.music.data

import android.content.Context
import com.pixelus.music.player.RepeatMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlayerStateManager(context: Context) {
    private val prefs = context.getSharedPreferences("saved_player_state", Context.MODE_PRIVATE)
    private val json = Json

    var savedPlaylistId: Long?
        get() = if (prefs.contains("playlist_id")) prefs.getLong("playlist_id", -1) else null
        set(value) { prefs.edit().putLong("playlist_id", value ?: -1).apply() }

    var savedSongId: Long?
        get() = if (prefs.contains("song_id")) prefs.getLong("song_id", -1) else null
        set(value) { prefs.edit().putLong("song_id", value ?: -1).apply() }

    var savedRepeatMode: RepeatMode
        get() = RepeatMode.entries[prefs.getInt("repeat_mode", 0)]
        set(value) { prefs.edit().putInt("repeat_mode", value.ordinal).apply() }

    var savedShuffleMode: Boolean
        get() = prefs.getBoolean("shuffle_mode", false)
        set(value) { prefs.edit().putBoolean("shuffle_mode", value).apply() }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
