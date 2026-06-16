package com.pixelus.music.data.playlist

import kotlinx.serialization.Serializable

@Serializable
data class LocalPlaylist(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val songIds: List<Long> = emptyList()
) {
    val songCount: Int get() = songIds.size
}
