package com.pixelus.music.ui.nowplaying

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.pixelus.music.data.Lyrics
import com.pixelus.music.player.PlayerState
import com.pixelus.music.player.RepeatMode
import com.pixelus.music.ui.theme.*
import com.pixelus.music.util.formatDuration

@Composable
fun NowPlayingScreen(
    playerState: PlayerState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onDominantColorChanged: (Color?) -> Unit = {},
    onStartSleepTimer: (Long) -> Unit = {},
    onStopSleepTimer: () -> Unit = {}
) {
    val song = playerState.currentSong ?: return
    var showLyrics by remember { mutableStateOf(false) }
    val hasLyrics = playerState.lyrics != null || playerState.lyricsLoading

    val context = LocalContext.current
    LaunchedEffect(song.albumArtUri) {
        onDominantColorChanged(null)
        song.albumArtUri?.let { uri ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(128)
                    .build()
                val result = Coil.imageLoader(context).execute(request)
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    onDominantColorChanged(extractDominantColor(bitmap))
                }
            } catch (_: Exception) {
                onDominantColorChanged(null)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        song.albumArtUri?.let { uri ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .size(64)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.85f),
                                Background
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
        ) {
            TopBar(
                onBack = onBack,
                title = song.title,
                hasLyrics = hasLyrics,
                showLyrics = showLyrics,
                onToggleLyrics = { showLyrics = !showLyrics },
                sleepTimerActive = playerState.sleepTimerActive,
                sleepTimerRemaining = playerState.sleepTimerRemaining,
                onStartSleepTimer = onStartSleepTimer,
                onStopSleepTimer = onStopSleepTimer
            )

            if (showLyrics) {
                Box(modifier = Modifier.weight(1f)) {
                    LyricsSection(
                        lyrics = playerState.lyrics,
                        isLoading = playerState.lyricsLoading,
                        currentPosition = playerState.currentPosition
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(0.2f))
                AlbumArtSection(albumArtUri = song.albumArtUri)
                Spacer(modifier = Modifier.weight(0.1f))
            }

            SongInfoSection(title = song.title, artist = song.artist, album = song.album)

            Spacer(modifier = Modifier.height(8.dp))

            SeekBarSection(
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlaybackControls(
                isPlaying = playerState.isPlaying,
                shuffleEnabled = playerState.shuffleEnabled,
                repeatMode = playerState.repeatMode,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onShuffle = onShuffle,
                onRepeat = onRepeat
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    title: String,
    hasLyrics: Boolean,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    sleepTimerActive: Boolean = false,
    sleepTimerRemaining: Long = 0,
    onStartSleepTimer: (Long) -> Unit = {},
    onStopSleepTimer: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse",
                tint = OnBackground
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (showLyrics) "LYRICS" else "NOW PLAYING",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        if (hasLyrics) {
            IconButton(onClick = onToggleLyrics) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = "Toggle Lyrics",
                    tint = if (showLyrics) Primary else OnBackground
                )
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = OnBackground
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (sleepTimerActive) {
                    DropdownMenuItem(
                        text = {
                            Text("Sleep timer: ${sleepTimerRemaining / 60000} min")
                        },
                        onClick = {
                            showMenu = false
                            onStopSleepTimer()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Sleep 15 min") },
                    onClick = {
                        showMenu = false
                        onStartSleepTimer(15 * 60 * 1000L)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sleep 30 min") },
                    onClick = {
                        showMenu = false
                        onStartSleepTimer(30 * 60 * 1000L)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sleep 45 min") },
                    onClick = {
                        showMenu = false
                        onStartSleepTimer(45 * 60 * 1000L)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sleep 1 hour") },
                    onClick = {
                        showMenu = false
                        onStartSleepTimer(60 * 60 * 1000L)
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumArtSection(albumArtUri: Uri?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = albumArtUri,
            contentDescription = "Album Art",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun LyricsSection(
    lyrics: Lyrics?,
    isLoading: Boolean,
    currentPosition: Long
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            lyrics == null -> {
                Text(
                    text = "No lyrics available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            lyrics.isSynced -> SyncedLyricsView(lyrics, currentPosition)
            else -> PlainLyricsView(lyrics)
        }
    }
}

@Composable
private fun SyncedLyricsView(lyrics: Lyrics, currentPosition: Long) {
    val listState = rememberLazyListState()
    val activeIndex = lyrics.lines.indexOfLast { it.timestampMs <= currentPosition }
        .coerceAtLeast(0)
    val settings = com.pixelus.music.PixelusApp.settings

    LaunchedEffect(activeIndex) {
        if (activeIndex > 0) {
            listState.animateScrollToItem(activeIndex - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == activeIndex
            Text(
                text = line.text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isActive) settings.lyricsFontSize else settings.lyricsFontSize.times(0.8f),
                    lineHeight = settings.lyricsLineHeight,
                    letterSpacing = settings.lyricsLetterSpacing,
                    color = if (isActive) OnBackground else TextSecondary
                ),
                textAlign = settings.lyricsAlignment,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PlainLyricsView(lyrics: Lyrics) {
    val settings = com.pixelus.music.PixelusApp.settings
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(lyrics.lines) { _, line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = settings.lyricsFontSize,
                    lineHeight = settings.lyricsLineHeight,
                    letterSpacing = settings.lyricsLetterSpacing
                ),
                textAlign = settings.lyricsAlignment,
                color = OnBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SongInfoSection(
    title: String,
    artist: String,
    album: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$artist \u2022 $album",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeekBarSection(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var sliderPosition by remember(currentPosition) {
        mutableFloatStateOf(if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                onSeek((sliderPosition * duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onShuffle) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) Primary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = OnBackground,
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(72.dp)
                .background(Primary, RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = OnPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = OnBackground,
                modifier = Modifier.size(36.dp)
            )
        }

        val repeatIcon = when (repeatMode) {
            RepeatMode.OFF -> Icons.Default.Repeat
            RepeatMode.ALL -> Icons.Default.Repeat
            RepeatMode.ONE -> Icons.Default.RepeatOne
        }
        IconButton(onClick = onRepeat) {
            Icon(
                imageVector = repeatIcon,
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) Primary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
