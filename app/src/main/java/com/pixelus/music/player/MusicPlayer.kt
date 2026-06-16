package com.pixelus.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.pixelus.music.PixelusApp
import com.pixelus.music.data.LyricsRepository
import com.pixelus.music.data.Song
import com.pixelus.music.widget.MusicServiceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@UnstableApi
class MusicPlayer(context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var songList: List<Song> = emptyList()
    private val lyricsRepo = LyricsRepository(context).apply {
        setAutoFetch(try { PixelusApp.settings.lyricsAutoFetch } catch (_: Exception) { true })
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val sleepTimer = SleepTimer()

    private var lastProgressUpdate = 0L

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                val song = _state.value.currentSong
                MusicServiceHolder.publishState(
                    title = song?.title ?: "No music playing",
                    artist = song?.artist ?: "",
                    album = song?.album ?: "",
                    albumArtUri = song?.albumArtUri?.toString(),
                    isPlaying = isPlaying,
                    positionMillis = exoPlayer.currentPosition
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(
                        duration = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exoPlayer.currentMediaItemIndex
                if (index in songList.indices) {
                    val song = songList[index]
                    _state.value = _state.value.copy(
                        currentSong = song,
                        queueIndex = index,
                        currentPosition = 0,
                        duration = exoPlayer.duration.coerceAtLeast(0),
                        lyrics = null,
                        lyricsLoading = true
                    )
                    MusicServiceHolder.publishState(
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        albumArtUri = song.albumArtUri?.toString(),
                        isPlaying = exoPlayer.isPlaying,
                        positionMillis = 0
                    )
                    savePlayerState()
                    loadLyrics(song)
                }
            }
        })
    }

    private fun loadLyrics(song: Song) {
        scope.launch {
            val lyrics = lyricsRepo.loadLyrics(song)
            _state.value = _state.value.copy(
                lyrics = lyrics,
                lyricsLoading = false
            )
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        songList = songs
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        _state.value = _state.value.copy(queue = songList, queueIndex = startIndex)
        savePlayerState()
    }

    fun playSong(song: Song, songs: List<Song>) {
        val index = songs.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            setQueue(songs, index)
        }
    }

    fun startSleepTimer(durationMs: Long) {
        sleepTimer.start(durationMs) {
            scope.launch(Dispatchers.Main) {
                exoPlayer.pause()
                _state.value = _state.value.copy(sleepTimerActive = false, sleepTimerRemaining = 0)
            }
        }
        _state.value = _state.value.copy(sleepTimerActive = true, sleepTimerRemaining = durationMs)
        scope.launch {
            while (sleepTimer.isActive) {
                _state.value = _state.value.copy(sleepTimerRemaining = sleepTimer.remainingMs)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun stopSleepTimer() {
        sleepTimer.stop()
        _state.value = _state.value.copy(sleepTimerActive = false, sleepTimerRemaining = 0)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _state.value = _state.value.copy(currentPosition = position)
    }

    fun skipToNext() {
        exoPlayer.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        val currentItemIndex = exoPlayer.currentMediaItemIndex
        val currentPosition = exoPlayer.currentPosition

        val jumpToBeginning = try { PixelusApp.settings.jumpToBeginning } catch (_: Exception) { true }
        if (jumpToBeginning && currentPosition > 3000 || currentItemIndex == 0) {
            exoPlayer.seekTo(currentItemIndex, 0)
        } else {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun getAudioSessionId(): Int = exoPlayer.audioSessionId

    fun toggleShuffle() {
        val newShuffle = !_state.value.shuffleEnabled
        exoPlayer.shuffleModeEnabled = newShuffle
        _state.value = _state.value.copy(shuffleEnabled = newShuffle)
    }

    fun cycleRepeatMode() {
        val newMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        exoPlayer.repeatMode = when (newMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _state.value = _state.value.copy(repeatMode = newMode)
    }

    fun addToQueue(songs: List<Song>) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        val insertIndex = exoPlayer.mediaItemCount
        exoPlayer.addMediaItems(insertIndex, mediaItems)
        songList = songList + songs
        _state.value = _state.value.copy(queue = songList)
        savePlayerState()
    }

    fun playNext(songs: List<Song>) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        val insertIndex = exoPlayer.currentMediaItemIndex + 1
        exoPlayer.addMediaItems(insertIndex, mediaItems)
        songList = songList.take(insertIndex) + songs + songList.drop(insertIndex)
        _state.value = _state.value.copy(queue = songList)
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= exoPlayer.mediaItemCount) return
        if (index == exoPlayer.currentMediaItemIndex) return // don't remove current
        exoPlayer.removeMediaItem(index)
        songList = songList.toMutableList().apply { removeAt(index) }
        _state.value = _state.value.copy(queue = songList)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        exoPlayer.moveMediaItem(fromIndex, toIndex)
        val mutable = songList.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        songList = mutable
        _state.value = _state.value.copy(queue = songList)
    }

    fun updateProgress() {
        _state.value = _state.value.copy(
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0),
            duration = exoPlayer.duration.coerceAtLeast(0)
        )
    }

    private fun savePlayerState() {
        try {
            val stateManager = com.pixelus.music.PixelusApp.playerStateManager
            stateManager.savedSongId = _state.value.currentSong?.id
            stateManager.savedRepeatMode = _state.value.repeatMode
            stateManager.savedShuffleMode = _state.value.shuffleEnabled
        } catch (_: Exception) { }
    }

    fun restorePlayerState(allSongs: List<Song>) {
        try {
            val stateManager = com.pixelus.music.PixelusApp.playerStateManager
            val songId = stateManager.savedSongId
            if (songId != null) {
                val song = allSongs.find { it.id == songId }
                if (song != null) {
                    val index = allSongs.indexOfFirst { it.id == songId }
                    setQueue(allSongs, index.coerceAtLeast(0))
                    exoPlayer.repeatMode = when (stateManager.savedRepeatMode) {
                        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    }
                    exoPlayer.shuffleModeEnabled = stateManager.savedShuffleMode
                }
            }
        } catch (_: Exception) { }
    }

    fun release() {
        exoPlayer.release()
    }
}
