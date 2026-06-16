package com.pixelus.music.data

import android.content.Context
import com.pixelus.music.player.RepeatMode

class PlayerStateManager(context: Context) {
    private val prefs = context.getSharedPreferences("saved_player_state", Context.MODE_PRIVATE)

    var savedQueueIndices: List<Long>
        get() = prefs.getString("queue_indices", null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
        set(value) {
            prefs.edit().putString("queue_indices", value.joinToString(",")).apply()
        }

    var savedQueueIndex: Int
        get() = prefs.getInt("queue_index", -1)
        set(value) { prefs.edit().putInt("queue_index", value).apply() }

    var savedPosition: Long
        get() = prefs.getLong("saved_position", 0)
        set(value) { prefs.edit().putLong("saved_position", value).apply() }

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

    var savedWasPlaying: Boolean
        get() = prefs.getBoolean("was_playing", false)
        set(value) { prefs.edit().putBoolean("was_playing", value).apply() }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
