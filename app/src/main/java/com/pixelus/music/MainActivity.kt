package com.pixelus.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelus.music.data.*
import com.pixelus.music.player.MusicService
import com.pixelus.music.ui.detail.DetailScreen
import com.pixelus.music.ui.library.*
import com.pixelus.music.ui.nowplaying.NowPlayingScreen
import com.pixelus.music.ui.search.SearchScreen
import com.pixelus.music.ui.theme.PixelusMusicTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy { MusicRepository(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> recreate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
        requestAudioPermission()

        setContent {
            PixelusMusicTheme {
                var songs by remember { mutableStateOf(emptyList<Song>()) }
                var albums by remember { mutableStateOf(emptyList<Album>()) }
                var artists by remember { mutableStateOf(emptyList<Artist>()) }
                var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
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
                    songs = repository.loadAllSongs()
                    albums = repository.loadAlbums()
                    artists = repository.loadArtists()
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
                }

                when {
                    detailSongs.isNotEmpty() -> {
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
                            onPlayerBarClick = { currentScreen = "now_playing" },
                            onPlayPause = { player.togglePlayPause() },
                            onSkipNext = { player.skipToNext() }
                        )
                    }
                    currentScreen == "now_playing" -> {
                        playerState.currentSong?.let {
                            NowPlayingScreen(
                                playerState = playerState,
                                onBack = { currentScreen = "library" },
                                onPlayPause = { player.togglePlayPause() },
                                onNext = { player.skipToNext() },
                                onPrevious = { player.skipToPrevious() },
                                onSeek = { player.seekTo(it) },
                                onShuffle = { player.toggleShuffle() },
                                onRepeat = { player.cycleRepeatMode() },
                                onStartSleepTimer = { player.startSleepTimer(it) },
                                onStopSleepTimer = { player.stopSleepTimer() }
                            )
                        }
                    }
                    currentScreen == "search" -> {
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
