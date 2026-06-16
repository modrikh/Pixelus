package com.pixelus.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelus.music.data.playlist.LocalPlaylist
import com.pixelus.music.player.MusicService
import com.pixelus.music.ui.detail.DetailScreen
import com.pixelus.music.ui.library.LibraryScreen
import com.pixelus.music.ui.navigation.Screen
import com.pixelus.music.ui.nowplaying.NowPlayingScreen
import com.pixelus.music.ui.search.SearchScreen
import com.pixelus.music.ui.settings.SettingsScreen
import com.pixelus.music.ui.setup.SetupWizard
import com.pixelus.music.ui.theme.PixelusMusicTheme
import com.pixelus.music.ui.viewmodel.LibraryViewModel

class MainActivity : ComponentActivity() {

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
            val paletteStyle by settings.paletteStyle.collectAsStateWithLifecycle()

            val viewModel: LibraryViewModel = viewModel()
            val uiState by viewModel.state.collectAsStateWithLifecycle()

            val effectiveDominantColor = if (useAlbumArtColor) uiState.dominantColor else null

            PixelusMusicTheme(
                dominantColor = effectiveDominantColor,
                appearance = appearance,
                useDynamicColor = useDynamicColor,
                amoledDarkMode = amoledMode,
                paletteStyle = paletteStyle
            ) {
                val player = MusicService.player
                val playerState by player.state.collectAsStateWithLifecycle()

                val repoPlaylists by PixelusApp.playlistRepository.playlists.collectAsStateWithLifecycle()
                LaunchedEffect(repoPlaylists) {
                    viewModel.onLocalPlaylistsUpdated(repoPlaylists)
                }

                when {
                    !PixelusApp.settings.setupComplete -> {
                        SetupWizard(
                            onComplete = {
                                PixelusApp.settings.setupComplete = true
                                viewModel.loadData()
                            }
                        )
                    }
                    else -> {
                        val currentScreen = uiState.currentScreen
                        BackHandler(
                            enabled = currentScreen !is Screen.Library
                        ) { viewModel.navigateBack() }

                        when (currentScreen) {
                            is Screen.Library -> {
                                LibraryScreen(
                                    songs = uiState.songs,
                                    albums = uiState.albums,
                                    artists = uiState.artists,
                                    playlists = uiState.mediaStorePlaylists,
                                    localPlaylists = uiState.localPlaylists,
                                    genres = uiState.genres,
                                    folders = uiState.folders,
                                    playerState = playerState,
                                    onSongClick = { song, songList ->
                                        viewModel.onSongClick(song, songList)
                                    },
                                    onAlbumClick = viewModel::onAlbumClick,
                                    onArtistClick = viewModel::onArtistClick,
                                    onPlaylistClick = viewModel::onPlaylistClick,
                                    onLocalPlaylistClick = viewModel::onLocalPlaylistClick,
                                    onGenreClick = viewModel::onGenreClick,
                                    onFolderClick = viewModel::onFolderClick,
                                    onSearchClick = { viewModel.navigateTo(Screen.Search) },
                                    onSettingsClick = { viewModel.navigateTo(Screen.Settings) },
                                    onPlayerBarClick = { viewModel.navigateTo(Screen.NowPlaying) },
                                    onPlayPause = { player.togglePlayPause() },
                                    onSkipNext = { player.skipToNext() },
                                    onRenamePlaylist = { id, name ->
                                        PixelusApp.playlistRepository.renamePlaylist(id, name)
                                    },
                                    onDeletePlaylist = { id ->
                                        PixelusApp.playlistRepository.deletePlaylist(id)
                                    }
                                )
                            }

                            is Screen.NowPlaying -> {
                                if (playerState.currentSong != null) {
                                    NowPlayingScreen(
                                        playerState = playerState,
                                        onBack = { viewModel.navigateBack() },
                                        onPlayPause = { player.togglePlayPause() },
                                        onNext = { player.skipToNext() },
                                        onPrevious = { player.skipToPrevious() },
                                        onSeek = { player.seekTo(it) },
                                        onShuffle = { player.toggleShuffle() },
                                        onRepeat = { player.cycleRepeatMode() },
                                        onDominantColorChanged = { viewModel.setDominantColor(it) },
                                        onStartSleepTimer = { player.startSleepTimer(it) },
                                        onStopSleepTimer = { player.stopSleepTimer() }
                                    )
                                }
                            }

                            is Screen.Settings -> {
                                SettingsScreen(
                                    onBack = { viewModel.navigateBack() },
                                    sleepTimerActive = playerState.sleepTimerActive,
                                    sleepTimerRemaining = playerState.sleepTimerRemaining,
                                    onStartSleepTimer = { player.startSleepTimer(it) },
                                    onStopSleepTimer = { player.stopSleepTimer() },
                                    songs = uiState.allSongs
                                )
                            }

                            is Screen.Search -> {
                                SearchScreen(
                                    songs = uiState.songs,
                                    onBack = { viewModel.navigateBack() },
                                    onSongClick = { song, songList ->
                                        viewModel.onSongClick(song, songList)
                                        viewModel.navigateTo(Screen.NowPlaying)
                                    }
                                )
                            }

                            is Screen.Detail -> {
                                DetailScreen(
                                    title = currentScreen.title,
                                    subtitle = currentScreen.subtitle,
                                    songs = currentScreen.songs,
                                    onBack = { viewModel.navigateBack() },
                                    onSongClick = { song, songList ->
                                        viewModel.onSongClick(song, songList)
                                    }
                                )
                            }
                        }
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
