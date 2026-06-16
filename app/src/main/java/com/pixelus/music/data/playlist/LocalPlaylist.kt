package com.pixelus.music.data.playlist

import com.pixelus.music.data.Song

data class LocalPlaylist(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val songs: List<Song> = emptyList()
)
