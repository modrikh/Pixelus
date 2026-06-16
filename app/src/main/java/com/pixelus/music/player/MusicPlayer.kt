package com.pixelus.music.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class MusicPlayer(context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .build()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var songList: List<Song> = emptyList()
    private val lyricsRepo = LyricsRepository(context).apply {
        setAutoFetch(try { PixelusApp.settings.lyricsAutoFetch } catch (_: Exception) { true })
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var positionJob: Job? = null

    val sleepTimer = SleepTimer()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseInternal()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusHeld = false

    private var pendingAudioFocusLoss = false

    private val audioFocusListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioFocusHeld = false
                pauseInternal()
                pendingAudioFocusLoss = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocusHeld = false
                if (exoPlayer.isPlaying) {
                    pauseInternal()
                    pendingAudioFocusLoss = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                exoPlayer.volume = if (exoPlayer.isPlaying) 0.2f else exoPlayer.volume
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusHeld = true
                exoPlayer.volume = 1f
                if (pendingAudioFocusLoss) {
                    pendingAudioFocusLoss = false
                }
            }
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
        }

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
                positionJob?.cancel()
                if (isPlaying) {
                    positionJob = scope.launch {
                        while (isActive) {
                            updateProgress()
                            val currentSong = _state.value.currentSong
                            MusicServiceHolder.publishState(
                                title = currentSong?.title ?: "No music playing",
                                artist = currentSong?.artist ?: "",
                                album = currentSong?.album ?: "",
                                albumArtUri = currentSong?.albumArtUri?.toString(),
                                isPlaying = true,
                                positionMillis = exoPlayer.currentPosition
                            )
                            delay(500L)
                        }
                    }
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

    private fun requestAudioFocus(): Boolean {
        if (!PixelusApp.settings.handleAudioFocus) {
            audioFocusHeld = true
            return true
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        audioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return audioFocusHeld
    }

    private fun abandonAudioFocus() {
        if (audioFocusHeld && PixelusApp.settings.handleAudioFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusListener)
            }
            audioFocusHeld = false
        }
    }

    private fun registerNoisyReceiver(context: Context) {
        if (!isNoisyReceiverRegistered) {
            context.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            isNoisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver(context: Context) {
        if (isNoisyReceiverRegistered) {
            try { context.unregisterReceiver(noisyReceiver) } catch (_: Exception) { }
            isNoisyReceiverRegistered = false
        }
    }

    private fun pauseInternal() {
        exoPlayer.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    private fun playInternal() {
        requestAudioFocus()
        registerNoisyReceiver(PixelusApp.instance)
        exoPlayer.play()
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

    fun setQueue(songs: List<Song>, startIndex: Int = 0, startPlaying: Boolean = true) {
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
        if (startPlaying) {
            playInternal()
        }
        _state.value = _state.value.copy(queue = songList, queueIndex = startIndex)
        savePlayerState()
    }

    fun playSong(song: Song, songs: List<Song>) {
        val index = songs.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            setQueue(songs, index, startPlaying = true)
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
                delay(1000)
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
            abandonAudioFocus()
            unregisterNoisyReceiver(PixelusApp.instance)
        } else {
            if (exoPlayer.mediaItemCount == 0) return
            playInternal()
        }
        savePlayerState()
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
        if (jumpToBeginning && currentPosition > 3000) {
            exoPlayer.seekTo(currentItemIndex, 0)
        } else if (currentItemIndex == 0) {
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
        savePlayerState()
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
        savePlayerState()
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
        savePlayerState()
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= exoPlayer.mediaItemCount) return
        if (index == exoPlayer.currentMediaItemIndex) return
        exoPlayer.removeMediaItem(index)
        songList = songList.toMutableList().apply { removeAt(index) }
        _state.value = _state.value.copy(queue = songList)
        savePlayerState()
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        exoPlayer.moveMediaItem(fromIndex, toIndex)
        val mutable = songList.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        songList = mutable
        _state.value = _state.value.copy(queue = songList)
        savePlayerState()
    }

    fun playAtIndex(index: Int) {
        if (index in songList.indices) {
            exoPlayer.seekToDefaultPosition(index)
            playInternal()
        }
    }

    fun updateProgress() {
        _state.value = _state.value.copy(
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0),
            duration = exoPlayer.duration.coerceAtLeast(0)
        )
    }

    fun savePlayerState() {
        try {
            val stateManager = PixelusApp.playerStateManager
            stateManager.savedSongId = _state.value.currentSong?.id
            stateManager.savedRepeatMode = _state.value.repeatMode
            stateManager.savedShuffleMode = _state.value.shuffleEnabled
            stateManager.savedQueueIndices = _state.value.queue.map { it.id }
            stateManager.savedQueueIndex = _state.value.queueIndex
            stateManager.savedPosition = exoPlayer.currentPosition
            stateManager.savedWasPlaying = exoPlayer.isPlaying
        } catch (_: Exception) { }
    }

    fun restorePlayerState(allSongs: List<Song>) {
        try {
            val stateManager = PixelusApp.playerStateManager
            val songId = stateManager.savedSongId
            if (songId != null) {
                val song = allSongs.find { it.id == songId }
                if (song != null) {
                    val savedIndices = stateManager.savedQueueIndices
                    val indices = if (savedIndices.isNotEmpty()) savedIndices else allSongs.map { it.id }
                    val restoredSongs = indices.mapNotNull { id -> allSongs.find { it.id == id } }
                    val startIndex = restoredSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)

                    songList = restoredSongs
                    val mediaItems = restoredSongs.map { s ->
                        MediaItem.Builder()
                            .setMediaId(s.id.toString())
                            .setUri(s.uri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(s.title)
                                    .setArtist(s.artist)
                                    .setAlbumTitle(s.album)
                                    .build()
                            )
                            .build()
                    }
                    exoPlayer.setMediaItems(mediaItems, startIndex, stateManager.savedPosition.coerceAtLeast(0))
                    exoPlayer.prepare()
                    exoPlayer.repeatMode = when (stateManager.savedRepeatMode) {
                        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    }
                    exoPlayer.shuffleModeEnabled = stateManager.savedShuffleMode

                    _state.value = _state.value.copy(
                        queue = restoredSongs,
                        queueIndex = startIndex,
                        currentPosition = stateManager.savedPosition.coerceAtLeast(0),
                        shuffleEnabled = stateManager.savedShuffleMode,
                        repeatMode = stateManager.savedRepeatMode
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayer", "Failed to restore player state", e)
        }
    }

    fun release() {
        unregisterNoisyReceiver(PixelusApp.instance)
        abandonAudioFocus()
        exoPlayer.release()
    }
}
