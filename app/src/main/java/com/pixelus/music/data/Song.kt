package com.pixelus.music.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val trackNumber: Int,
    val year: Int,
    val genre: String,
    val uri: Uri,
    val albumArtUri: Uri?,
    val folderPath: String,
    val dateAdded: Long
)
