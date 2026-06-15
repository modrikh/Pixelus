package com.pixelus.music.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun loadAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val c = context.contentResolver.query(uri, projection, selection, null, sortOrder) ?: return@withContext emptyList()
        c.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown"
                val artist = it.getString(artistCol) ?: "Unknown Artist"
                val album = it.getString(albumCol) ?: "Unknown Album"
                val albumId = it.getLong(albumIdCol)
                val duration = it.getLong(durationCol)
                val track = it.getInt(trackCol)
                val year = it.getInt(yearCol)
                val data = it.getString(dataCol) ?: ""
                val dateAdded = it.getLong(dateCol)

                val songUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )

                val genre = getGenreForSong(context, id)

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        duration = duration,
                        trackNumber = track,
                        year = year,
                        genre = genre,
                        uri = songUri,
                        albumArtUri = albumArtUri,
                        folderPath = data.substringBeforeLast("/"),
                        dateAdded = dateAdded
                    )
                )
            }
        }
        songs
    }

    suspend fun loadAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val songs = loadAllSongs()
        songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
            val first = albumSongs.first()
            Album(
                id = albumId,
                title = first.album,
                artist = first.artist,
                songCount = albumSongs.size,
                year = albumSongs.maxOf { it.year },
                albumArtUri = first.albumArtUri,
                songs = albumSongs
            )
        }.sortedBy { it.title }
    }

    suspend fun loadArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )
        val artists = mutableListOf<Artist>()
        val artistCursor = context.contentResolver.query(uri, projection, null, null, null) ?: return@withContext artists
        artistCursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            while (c.moveToNext()) {
                artists.add(
                    Artist(
                        id = c.getLong(idCol),
                        name = c.getString(nameCol) ?: "Unknown",
                        albumCount = c.getInt(albumCol),
                        songCount = c.getInt(trackCol)
                    )
                )
            }
        }
        artists
    }

    suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
        )
        val playlists = mutableListOf<Playlist>()
        val playlistCursor = context.contentResolver.query(uri, projection, null, null, null) ?: return@withContext playlists
        playlistCursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "Unknown"
                val count = getPlaylistSongCount(context, id)
                playlists.add(Playlist(id, name, count))
            }
        }
        playlists
    }

    private fun getGenreForSong(context: Context, songId: Long): String {
        val uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", (songId % Int.MAX_VALUE).toInt())
        val projection = arrayOf(MediaStore.Audio.Genres.NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)) ?: "Unknown"
            }
        }
        return "Unknown"
    }

    suspend fun loadPlaylistSongs(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        val projection = arrayOf(
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Playlists.Members.PLAY_ORDER
        )
        val songIds = mutableListOf<Long>()
        val c = context.contentResolver.query(uri, projection, null, null, "${MediaStore.Audio.Playlists.Members.PLAY_ORDER} ASC")
        c?.use {
            val audioIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)
            while (it.moveToNext()) {
                songIds.add(it.getLong(audioIdCol))
            }
        }
        loadAllSongs().filter { it.id in songIds }
    }

    private fun getPlaylistSongCount(context: Context, playlistId: Long): Int {
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            return cursor.count
        }
        return 0
    }
}
