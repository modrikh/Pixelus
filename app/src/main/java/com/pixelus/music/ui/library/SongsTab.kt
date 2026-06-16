package com.pixelus.music.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelus.music.PixelusApp
import com.pixelus.music.R
import com.pixelus.music.data.Song
import com.pixelus.music.data.SongSort
import com.pixelus.music.player.MusicPlayer
import com.pixelus.music.ui.components.ScrollToTopAndLocateButtons
import com.pixelus.music.ui.theme.*
import com.pixelus.music.ui.theme.Dimens
import com.pixelus.music.util.formatDuration

@Composable
fun SongsTab(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val settings = PixelusApp.settings
    val currentSort by settings.trackSort.collectAsState()
    val ascending by settings.trackSortAscending.collectAsState()

    val sortedSongs = remember(songs, currentSort, ascending) {
        sortSongs(songs, currentSort, ascending)
    }

    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSizeLarge),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.height(Dimens.spacingSmall))
                Text("No songs found", color = TextSecondary)
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SortBar(
                currentSort = currentSort,
                ascending = ascending,
                onSortChange = { settings.updateTrackSort(it) },
                onOrderToggle = { settings.updateTrackSortAscending(!ascending) }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(sortedSongs) { index, song ->
                    SongItemWithMenu(
                        song = song,
                        onClick = { onSongClick(song, sortedSongs) }
                    )
                }
            }
        }

        ScrollToTopAndLocateButtons(
            showScrollToTopButton = listState.firstVisibleItemIndex > 0,
            onScrollToTopClick = {
                coroutineScope.launch { listState.animateScrollToItem(0) }
            },
            showLocateButton = false,
            onLocateClick = {},
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItemWithMenu(
    song: Song,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = Surface,
        shape = RoundedCornerShape(Dimens.cornerRadiusMedium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spacingSmall + 4.dp, vertical = Dimens.spacingExtraSmall)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingSmall + 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .size(Dimens.albumArtMedium - 8.dp)
                    .clip(RoundedCornerShape(Dimens.cornerRadiusSmall)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_music_note)
            )

            Spacer(modifier = Modifier.width(Dimens.spacingSmall + 4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} \u2022 ${song.album}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Box {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = DpOffset(x = (-100).dp, y = 0.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next") },
                        onClick = {
                            showMenu = false
                            runCatching {
                                com.pixelus.music.player.MusicService.player.playNext(listOf(song))
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue") },
                        onClick = {
                            showMenu = false
                            runCatching {
                                com.pixelus.music.player.MusicService.player.addToQueue(listOf(song))
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBar(
    currentSort: SongSort,
    ascending: Boolean,
    onSortChange: (SongSort) -> Unit,
    onOrderToggle: () -> Unit
) {
    Surface(
        color = Background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SongSort.entries.forEach { sort ->
                    val isSelected = sort == currentSort
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSortChange(sort) },
                        label = {
                            Text(
                                sort.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.2f),
                            selectedLabelColor = Primary
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onOrderToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (ascending) Icons.Default.ArrowUpward
                    else Icons.Default.ArrowDownward,
                    contentDescription = if (ascending) "Ascending" else "Descending",
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun sortSongs(songs: List<Song>, sort: SongSort, ascending: Boolean): List<Song> {
    val sorted = when (sort) {
        SongSort.TITLE -> songs.sortedBy { it.title.lowercase() }
        SongSort.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        SongSort.ALBUM -> songs.sortedBy { it.album.lowercase() }
        SongSort.DURATION -> songs.sortedBy { it.duration }
        SongSort.DATE_ADDED -> songs.sortedBy { it.dateAdded }
        SongSort.YEAR -> songs.sortedBy { it.year }
    }
    return if (ascending) sorted else sorted.reversed()
}
