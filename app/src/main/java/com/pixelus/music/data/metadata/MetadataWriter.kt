package com.pixelus.music.data.metadata

import android.content.IntentSender
import com.pixelus.music.data.Song
import com.pixelus.music.data.result.DataError
import com.pixelus.music.data.result.Result

interface MetadataWriter {
    val unsupportedArtworkEditFormats: List<String>

    fun readMetadata(song: Song): Result<Metadata, DataError>

    fun writeMetadata(
        song: Song,
        metadata: Metadata,
        onSecurityError: (IntentSender) -> Unit
    ): Result<Unit, DataError>

    fun writeLyricsToTag(song: Song, lyrics: String): Result<Unit, DataError>

    fun deleteLyricsFromTag(song: Song): Result<Unit, DataError>
}
