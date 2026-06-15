package com.pixelus.music.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelus.music.data.Album
import com.pixelus.music.data.Artist
import com.pixelus.music.data.Genre
import com.pixelus.music.data.MusicFolder
import com.pixelus.music.data.Playlist
import com.pixelus.music.data.Song
import com.pixelus.music.player.PlayerState
import com.pixelus.music.ui.theme.*

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    genres: List<Genre>,
    folders: List<MusicFolder>,
    playerState: PlayerState?,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onGenreClick: (Genre) -> Unit,
    onFolderClick: (MusicFolder) -> Unit,
    onSearchClick: () -> Unit,
    onPlayerBarClick: () -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pixelus Music",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground
                )
            )
        },
        bottomBar = {
            Column {
                if (playerState?.currentSong != null) {
                    PlayerBar(
                        playerState = playerState,
                        onClick = onPlayerBarClick,
                        onPlayPause = onPlayPause,
                        onSkipNext = onSkipNext
                    )
                }
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Background,
                    contentColor = Primary,
                    indicator = { tabPositions ->
                        if (selectedTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                color = Primary
                            )
                        }
                    }
                ) {
                    LibraryTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = MaterialTheme.typography.labelLarge.fontSize
                                )
                            },
                            selectedContentColor = Primary,
                            unselectedContentColor = TextSecondary
                        )
                    }
                }
            }
        },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                LibraryTab.SONGS -> SongsTab(songs = songs, onSongClick = onSongClick)
                LibraryTab.ALBUMS -> AlbumsTab(albums = albums, onAlbumClick = onAlbumClick)
                LibraryTab.ARTISTS -> ArtistsTab(artists = artists, onArtistClick = onArtistClick)
                LibraryTab.GENRES -> GenresTab(genres = genres, onGenreClick = onGenreClick)
                LibraryTab.PLAYLISTS -> PlaylistsTab(playlists = playlists, onPlaylistClick = onPlaylistClick)
                LibraryTab.FOLDERS -> FoldersTab(folders = folders, onFolderClick = onFolderClick)
            }
        }
    }
}

@Composable
fun PlayerBar(
    playerState: PlayerState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val albumArtUri = playerState.currentSong?.albumArtUri
            if (albumArtUri != null) {
                AsyncImage(
                    model = albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerState.currentSong?.title ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = playerState.currentSong?.artist ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = Primary
                )
            }

            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = OnBackground
                )
            }
        }
    }
}
