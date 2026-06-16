package com.pixelus.music.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelus.music.PixelusApp
import com.pixelus.music.data.*
import com.pixelus.music.data.playlist.LocalPlaylist
import com.pixelus.music.player.MusicService
import com.pixelus.music.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val allSongs: List<Song> = emptyList(),
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val mediaStorePlaylists: List<Playlist> = emptyList(),
    val localPlaylists: List<LocalPlaylist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val folders: List<MusicFolder> = emptyList(),
    val isLoading: Boolean = true,
    val currentScreen: Screen = Screen.Library,
    val dominantColor: androidx.compose.ui.graphics.Color? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val settings = PixelusApp.settings
    private val player get() = MusicService.player

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val allSongs = repository.loadAllSongs()
            val ignoreShort = settings.ignoreShortTracks
            val songs = (if (ignoreShort) allSongs.filter { it.duration >= 30000 } else allSongs)
                .let { filterByFolderSettings(it) }

            val albums = buildAlbums(songs)
            val artists = buildArtists(songs)
            val mediaStorePlaylists = repository.loadPlaylists()
            val genres = buildGenres(songs)
            val folders = buildFolders(songs)

            _state.update {
                it.copy(
                    allSongs = allSongs,
                    songs = songs,
                    albums = albums,
                    artists = artists,
                    mediaStorePlaylists = mediaStorePlaylists,
                    genres = genres,
                    folders = folders,
                    isLoading = false
                )
            }
        }
    }

    private fun buildAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
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

    private fun buildArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { it.artist }.map { (name, artistSongs) ->
            val artistAlbums = artistSongs.distinctBy { it.albumId }
            Artist(
                id = artistSongs.first().id,
                name = name,
                albumCount = artistAlbums.size,
                songCount = artistSongs.size
            )
        }.sortedBy { it.name }
    }

    private fun buildGenres(songs: List<Song>): List<Genre> {
        return songs.groupBy { it.genre }.map { Genre(it.key, it.value.size) }
    }

    private fun buildFolders(songs: List<Song>): List<MusicFolder> {
        return songs.groupBy { it.folderPath }.map {
            MusicFolder(
                path = it.key,
                songCount = it.value.size,
                name = it.key.substringAfterLast("/")
            )
        }
    }

    private fun filterByFolderSettings(songs: List<Song>): List<Song> {
        val isInclusive = settings.isScanModeInclusive.value
        val musicFolder = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_MUSIC
        ).absolutePath
        val scanMusicFolder = settings.scanMusicFolder.value

        return if (isInclusive) {
            val allowedFolders = settings.extraScanFolders.value.toMutableSet()
            if (scanMusicFolder) allowedFolders.add(musicFolder)
            if (allowedFolders.isEmpty()) return songs
            songs.filter { song ->
                allowedFolders.any { song.folderPath.startsWith(it) }
            }
        } else {
            val excludedFolders = settings.excludedScanFolders.value
            if (excludedFolders.isEmpty()) return songs
            songs.filter { song ->
                excludedFolders.none { song.folderPath.startsWith(it) }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _state.update { it.copy(currentScreen = screen) }
    }

    fun navigateBack() {
        _state.update { it.copy(currentScreen = Screen.Library) }
    }

    fun onSongClick(song: Song, songList: List<Song>) {
        player.playSong(song, songList)
    }

    fun onAlbumClick(album: Album) {
        _state.update {
            it.copy(
                currentScreen = Screen.Detail(
                    title = album.title,
                    subtitle = "${album.artist} \u2022 ${album.songCount} songs",
                    songs = album.songs
                )
            )
        }
    }

    fun onArtistClick(artist: Artist) {
        _state.update {
            it.copy(
                currentScreen = Screen.Detail(
                    title = artist.name,
                    subtitle = "${artist.albumCount} albums \u2022 ${artist.songCount} songs",
                    songs = _state.value.songs.filter { s -> s.artist == artist.name }
                )
            )
        }
    }

    fun onPlaylistClick(playlist: Playlist) {
        viewModelScope.launch {
            val songs = repository.loadPlaylistSongs(playlist.id)
            _state.update {
                it.copy(
                    currentScreen = Screen.Detail(
                        title = playlist.name,
                        subtitle = "${playlist.songCount} songs",
                        songs = songs
                    )
                )
            }
        }
    }

    fun onLocalPlaylistClick(localPlaylist: LocalPlaylist) {
        _state.update {
            it.copy(
                currentScreen = Screen.Detail(
                    title = localPlaylist.name,
                    subtitle = "${localPlaylist.songCount} songs",
                    songs = _state.value.songs.filter { s -> s.id in localPlaylist.songIds }
                )
            )
        }
    }

    fun onGenreClick(genre: Genre) {
        _state.update {
            it.copy(
                currentScreen = Screen.Detail(
                    title = genre.name,
                    subtitle = "${genre.songCount} songs",
                    songs = _state.value.songs.filter { s -> s.genre == genre.name }
                )
            )
        }
    }

    fun onFolderClick(folder: MusicFolder) {
        _state.update {
            it.copy(
                currentScreen = Screen.Detail(
                    title = folder.name,
                    subtitle = "${folder.songCount} songs",
                    songs = _state.value.songs.filter { s -> s.folderPath == folder.path }
                )
            )
        }
    }

    fun setDominantColor(color: androidx.compose.ui.graphics.Color?) {
        _state.update { it.copy(dominantColor = color) }
    }

    fun onLocalPlaylistsUpdated(playlists: List<LocalPlaylist>) {
        _state.update { it.copy(localPlaylists = playlists) }
    }
}
