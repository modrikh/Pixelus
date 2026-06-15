package com.pixelus.music.player

import com.pixelus.music.data.Lyrics
import com.pixelus.music.data.Song

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val lyrics: Lyrics? = null,
    val lyricsLoading: Boolean = false,
    val sleepTimerActive: Boolean = false,
    val sleepTimerRemaining: Long = 0
)

enum class RepeatMode {
    OFF, ALL, ONE
}
