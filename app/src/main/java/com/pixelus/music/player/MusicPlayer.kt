package com.pixelus.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
    private val lyricsRepo = LyricsRepository(context)
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
    }

    fun playSong(song: Song, songs: List<Song>) {
        val index = songs.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            setQueue(songs, index)
        }
    }

    fun startSleepTimer(durationMs: Long) {
        sleepTimer.start(durationMs) {
            exoPlayer.pause()
            _state.value = _state.value.copy(sleepTimerActive = false, sleepTimerRemaining = 0)
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

        if (currentPosition > 3000 || currentItemIndex == 0) {
            exoPlayer.seekTo(currentItemIndex, 0)
        } else {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

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

    fun updateProgress() {
        _state.value = _state.value.copy(
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0),
            duration = exoPlayer.duration.coerceAtLeast(0)
        )
    }

    fun release() {
        exoPlayer.release()
    }
}
