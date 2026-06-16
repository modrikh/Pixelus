package com.pixelus.music.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelus.music.PixelusApp
import com.pixelus.music.data.Album
import com.pixelus.music.data.Artist
import com.pixelus.music.data.Genre
import com.pixelus.music.data.MusicFolder
import com.pixelus.music.data.Playlist
import com.pixelus.music.data.Song
import com.pixelus.music.data.playlist.LocalPlaylist
import com.pixelus.music.player.PlayerState
import com.pixelus.music.ui.components.Tab
import com.pixelus.music.ui.theme.*

private fun Tab.icon(): ImageVector = when (this) {
    Tab.Tracks -> Icons.Outlined.MusicNote
    Tab.Albums -> Icons.Outlined.Album
    Tab.Artists -> Icons.Outlined.Person
    Tab.Genres -> Icons.Outlined.Category
    Tab.Playlists -> Icons.Outlined.QueueMusic
    Tab.Folders -> Icons.Outlined.Folder
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    localPlaylists: List<LocalPlaylist>,
    genres: List<Genre>,
    folders: List<MusicFolder>,
    playerState: PlayerState?,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onLocalPlaylistClick: (LocalPlaylist) -> Unit,
    onGenreClick: (Genre) -> Unit,
    onFolderClick: (MusicFolder) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onPlayerBarClick: () -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onRenamePlaylist: (Long, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (Long) -> Unit = {}
) {
    val settings = PixelusApp.settings
    val tabOrder by settings.tabOrder.collectAsState()
    val gridPlaylists by settings.gridPlaylists.collectAsState()
    val replaceSearchWithFilter by settings.replaceSearchWithFilter.collectAsState()

    val initialTab = remember(tabOrder) {
        if (settings.defaultTab in tabOrder) settings.defaultTab else tabOrder.firstOrNull() ?: Tab.Tracks
    }
    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }

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
                    if (replaceSearchWithFilter) {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    } else {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                    selectedTabIndex = tabOrder.indexOf(selectedTab).coerceAtLeast(0),
                    containerColor = Background,
                    contentColor = Primary,
                    indicator = { tabPositions ->
                        val idx = tabOrder.indexOf(selectedTab).coerceAtLeast(0)
                        if (idx < tabPositions.size) {
                            @Suppress("DEPRECATION")
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                                color = Primary
                            )
                        }
                    }
                ) {
                    tabOrder.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon(),
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
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
                Tab.Tracks -> SongsTab(songs = songs, onSongClick = onSongClick, onPlayNext = { song -> }, onAddToQueue = { song -> })
                Tab.Albums -> AlbumsTab(albums = albums, onAlbumClick = onAlbumClick)
                Tab.Artists -> ArtistsTab(artists = artists, onArtistClick = onArtistClick)
                Tab.Genres -> GenresTab(genres = genres, onGenreClick = onGenreClick)
                Tab.Playlists -> PlaylistsTab(
                    playlists = playlists,
                    localPlaylists = localPlaylists,
                    onPlaylistClick = onPlaylistClick,
                    onLocalPlaylistClick = onLocalPlaylistClick,
                    gridView = gridPlaylists,
                    onRenamePlaylist = onRenamePlaylist,
                    onDeletePlaylist = onDeletePlaylist
                )
                Tab.Folders -> FoldersTab(folders = folders, onFolderClick = onFolderClick)
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
        color = Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                        .clip(RoundedCornerShape(8.dp)),
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
                    color = TextSecondary,
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
