package com.pixelus.music.data.remote

import com.pixelus.music.data.Lyrics
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LrclibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

object LrclibClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient()

    suspend fun fetchLyrics(title: String, artist: String, album: String, durationMs: Long): Lyrics? {
        return try {
            val response = client.get("https://lrclib.net/api/get") {
                parameter("track_name", title)
                parameter("artist_name", artist)
                parameter("album_name", album)
            }
            if (response.status.value != 200) return null

            val body = response.bodyAsText()
            val data = json.decodeFromString<LrclibResponse>(body)

            val synced = data.syncedLyrics
            val plain = data.plainLyrics

            when {
                !synced.isNullOrBlank() -> Lyrics.fromLrc(synced)
                !plain.isNullOrBlank() -> Lyrics.fromPlainText(plain)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        client.close()
    }
}
