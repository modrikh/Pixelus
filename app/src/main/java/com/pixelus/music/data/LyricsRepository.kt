package com.pixelus.music.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import com.pixelus.music.data.remote.LrclibClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LyricsRepository(private val context: Context) {

    suspend fun loadLyrics(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val embedded = extractEmbedded(song)
        if (embedded != null) return@withContext embedded

        val lrc = loadSidecar(song, "lrc")
        if (lrc != null) return@withContext lrc

        val txt = loadSidecar(song, "txt")
        if (txt != null) return@withContext txt

        try {
            LrclibClient.fetchLyrics(
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.duration
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractEmbedded(song: Song): Lyrics? {
        if (Build.VERSION.SDK_INT < 29) return null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, song.uri)
            val raw = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_LYRICS
            ) ?: return null
            if (raw.isBlank()) return null
            return Lyrics.fromEmbedded(raw)
        } catch (_: Exception) {
            return null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun loadSidecar(song: Song, ext: String): Lyrics? {
        val audioFile = File(song.uri.path ?: return null)
        val sidecar = audioFile.resolveSibling("${audioFile.nameWithoutExtension}.$ext")
        if (!sidecar.exists()) return null
        val content = try {
            sidecar.readText()
        } catch (_: Exception) {
            return null
        }
        if (content.isBlank()) return null
        return when (ext) {
            "lrc" -> Lyrics.fromLrc(content)
            else -> Lyrics.fromPlainText(content)
        }
    }
}
