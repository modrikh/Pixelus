package com.pixelus.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelus.music.data.*
import com.pixelus.music.data.playlist.LocalPlaylist
import com.pixelus.music.player.MusicService
import com.pixelus.music.ui.detail.DetailScreen
import com.pixelus.music.ui.library.*
import com.pixelus.music.ui.nowplaying.NowPlayingScreen
import com.pixelus.music.ui.search.SearchScreen
import com.pixelus.music.ui.settings.SettingsScreen
import com.pixelus.music.ui.theme.PixelusMusicTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy { MusicRepository(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> recreate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(Intent(this, MusicService::class.java))
        requestAudioPermission()

        setContent {
            val settings = PixelusApp.settings
            val useAlbumArtColor by settings.useAlbumArtColor.collectAsStateWithLifecycle()
            val appearance by settings.appearance.collectAsStateWithLifecycle()
            val useDynamicColor by settings.useDynamicColor.collectAsStateWithLifecycle()
            val amoledMode by settings.amoledDarkTheme.collectAsStateWithLifecycle()

            var dominantColor by remember { mutableStateOf<Color?>(null) }
            val effectiveDominantColor = if (useAlbumArtColor) dominantColor else null

            val paletteStyle by settings.paletteStyle.collectAsStateWithLifecycle()

            PixelusMusicTheme(
                dominantColor = effectiveDominantColor,
                appearance = appearance,
                useDynamicColor = useDynamicColor,
                amoledDarkMode = amoledMode,
                paletteStyle = paletteStyle
            ) {
                var allSongs by remember { mutableStateOf(emptyList<Song>()) }
                var songs by remember { mutableStateOf(emptyList<Song>()) }
                var albums by remember { mutableStateOf(emptyList<Album>()) }
                var artists by remember { mutableStateOf(emptyList<Artist>()) }
                var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
                var localPlaylists by remember { mutableStateOf(emptyList<LocalPlaylist>()) }
                var genres by remember { mutableStateOf(emptyList<Genre>()) }
                var folders by remember { mutableStateOf(emptyList<MusicFolder>()) }
                var currentScreen by remember { mutableStateOf("library") }

                var detailTitle by remember { mutableStateOf("") }
                var detailSubtitle by remember { mutableStateOf<String?>(null) }
                var detailSongs by remember { mutableStateOf(emptyList<Song>()) }

                val player = MusicService.player
                val playerState by player.state.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()

                suspend fun loadAll() {
                    allSongs = repository.loadAllSongs()
                    val ignoreShort = settings.ignoreShortTracks
                    songs = (if (ignoreShort) allSongs.filter { it.duration >= 30000 } else allSongs)
                        .let { filterByFolderSettings(it, settings) }

                    albums = songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
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

                    artists = songs.groupBy { it.artist }.map { (name, artistSongs) ->
                        val artistAlbums = artistSongs.distinctBy { it.albumId }
                        Artist(
                            id = artistSongs.first().id,
                            name = name,
                            albumCount = artistAlbums.size,
                            songCount = artistSongs.size
                        )
                    }.sortedBy { it.name }

                    playlists = repository.loadPlaylists()

                    val genreMap = songs.groupBy { it.genre }
                    genres = genreMap.map { Genre(it.key, it.value.size) }

                    val folderMap = songs.groupBy { it.folderPath }
                    folders = folderMap.map {
                        MusicFolder(
                            path = it.key,
                            songCount = it.value.size,
                            name = it.key.substringAfterLast("/")
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    loadAll()
                    player.restorePlayerState(allSongs)
                }

                val repoPlaylists by PixelusApp.playlistRepository.playlists.collectAsStateWithLifecycle()
                LaunchedEffect(repoPlaylists) { localPlaylists = repoPlaylists }

                when {
                    detailSongs.isNotEmpty() -> {
                        BackHandler { detailSongs = emptyList(); currentScreen = "library" }
                        DetailScreen(
                            title = detailTitle,
                            subtitle = detailSubtitle,
                            songs = detailSongs,
                            onBack = {
                                detailSongs = emptyList()
                                currentScreen = "library"
                            },
                            onSongClick = { song, songList ->
                                player.playSong(song, songList)
                            }
                        )
                    }
                    currentScreen == "library" -> {
                        LibraryScreen(
                            songs = songs,
                            albums = albums,
                            artists = artists,
                            playlists = playlists,
                            localPlaylists = localPlaylists,
                            genres = genres,
                            folders = folders,
                            playerState = playerState,
                            onSongClick = { song, songList ->
                                player.playSong(song, songList)
                            },
                            onAlbumClick = { album ->
                                detailTitle = album.title
                                detailSubtitle = "${album.artist} \u2022 ${album.songCount} songs"
                                detailSongs = album.songs
                                currentScreen = "detail"
                            },
                            onArtistClick = { artist ->
                                detailTitle = artist.name
                                detailSubtitle = "${artist.albumCount} albums \u2022 ${artist.songCount} songs"
                                detailSongs = songs.filter { it.artist == artist.name }
                                currentScreen = "detail"
                            },
                            onPlaylistClick = { playlist ->
                                detailTitle = playlist.name
                                detailSubtitle = "${playlist.songCount} songs"
                                currentScreen = "detail"
                                scope.launch {
                                    detailSongs = repository.loadPlaylistSongs(playlist.id)
                                }
                            },
                            onLocalPlaylistClick = { localPlaylist ->
                                detailTitle = localPlaylist.name
                                detailSubtitle = "${localPlaylist.songs.size} songs"
                                detailSongs = localPlaylist.songs
                                currentScreen = "detail"
                            },
                            onGenreClick = { genre ->
                                detailTitle = genre.name
                                detailSubtitle = "${genre.songCount} songs"
                                detailSongs = songs.filter { it.genre == genre.name }
                                currentScreen = "detail"
                            },
                            onFolderClick = { folder ->
                                detailTitle = folder.name
                                detailSubtitle = "${folder.songCount} songs"
                                detailSongs = songs.filter { it.folderPath == folder.path }
                                currentScreen = "detail"
                            },
                            onSearchClick = { currentScreen = "search" },
                            onSettingsClick = { currentScreen = "settings" },
                            onPlayerBarClick = { currentScreen = "now_playing" },
                            onPlayPause = { player.togglePlayPause() },
                            onSkipNext = { player.skipToNext() },
                            onRenamePlaylist = { id, name -> PixelusApp.playlistRepository.renamePlaylist(id, name) },
                            onDeletePlaylist = { id -> PixelusApp.playlistRepository.deletePlaylist(id) }
                        )
                    }
                    currentScreen == "now_playing" -> {
                        BackHandler { currentScreen = "library" }
                        if (playerState.currentSong != null) {
                            NowPlayingScreen(
                                playerState = playerState,
                                onBack = { currentScreen = "library" },
                                onPlayPause = { player.togglePlayPause() },
                                onNext = { player.skipToNext() },
                                onPrevious = { player.skipToPrevious() },
                                onSeek = { player.seekTo(it) },
                                onShuffle = { player.toggleShuffle() },
                                onRepeat = { player.cycleRepeatMode() },
                                onDominantColorChanged = { dominantColor = it },
                                onStartSleepTimer = { player.startSleepTimer(it) },
                                onStopSleepTimer = { player.stopSleepTimer() }
                            )
                        }
                    }
                    currentScreen == "settings" -> {
                        BackHandler { currentScreen = "library" }
                        SettingsScreen(
                            onBack = { currentScreen = "library" },
                            sleepTimerActive = playerState.sleepTimerActive,
                            sleepTimerRemaining = playerState.sleepTimerRemaining,
                            onStartSleepTimer = { player.startSleepTimer(it) },
                            onStopSleepTimer = { player.stopSleepTimer() },
                            songs = allSongs
                        )
                    }
                    currentScreen == "search" -> {
                        BackHandler { currentScreen = "library" }
                        SearchScreen(
                            songs = songs,
                            onBack = { currentScreen = "library" },
                            onSongClick = { song, songList ->
                                player.playSong(song, songList)
                                currentScreen = "now_playing"
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(permission)
        }
    }
}

private fun filterByFolderSettings(songs: List<Song>, settings: PixelusSettings): List<Song> {
    val isInclusive = settings.isScanModeInclusive.value
    val musicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
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
