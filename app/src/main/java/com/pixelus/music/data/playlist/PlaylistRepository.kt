package com.pixelus.music.data.playlist

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlaylistRepository(context: Context) {
    private val prefs = context.getSharedPreferences("local_playlists", Context.MODE_PRIVATE)
    private val json = Json

    private val _playlists = MutableStateFlow(loadAll())
    val playlists = _playlists.asStateFlow()

    private fun loadAll(): List<LocalPlaylist> {
        val size = prefs.getInt("playlist_count", 0)
        return (0 until size).mapNotNull { i ->
            val data = prefs.getString("playlist_$i", null) ?: return@mapNotNull null
            try { json.decodeFromString<LocalPlaylist>(data) }
            catch (_: Exception) { null }
        }
    }

    private fun saveAll(playlists: List<LocalPlaylist>) {
        prefs.edit().apply {
            putInt("playlist_count", playlists.size)
            playlists.forEachIndexed { i, p ->
                putString("playlist_$i", json.encodeToString(p))
            }
            apply()
        }
    }

    fun createPlaylist(name: String): LocalPlaylist {
        val playlist = LocalPlaylist(name = name)
        val updated = _playlists.value + playlist
        _playlists.value = updated
        saveAll(updated)
        return playlist
    }

    fun renamePlaylist(id: Long, newName: String) {
        val updated = _playlists.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
        _playlists.value = updated
        saveAll(updated)
    }

    fun deletePlaylist(id: Long) {
        val updated = _playlists.value.filter { it.id != id }
        _playlists.value = updated
        saveAll(updated)
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        val updated = _playlists.value.map {
            if (it.id == playlistId) {
                val existing = it.songs.toMutableSet()
                it.copy(songs = (it.songs + songs).distinctBy { s -> s.id })
            } else it
        }
        _playlists.value = updated
        saveAll(updated)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        val updated = _playlists.value.map {
            if (it.id == playlistId) it.copy(songs = it.songs.filter { s -> s.id != songId })
            else it
        }
        _playlists.value = updated
        saveAll(updated)
    }

    fun reorderPlaylist(playlistId: Long, songs: List<Song>) {
        val updated = _playlists.value.map {
            if (it.id == playlistId) it.copy(songs = songs) else it
        }
        _playlists.value = updated
        saveAll(updated)
    }
}
