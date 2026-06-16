package com.pixelus.music.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelus.music.PixelusApp
import com.pixelus.music.data.Playlist
import com.pixelus.music.data.PlaylistSort
import com.pixelus.music.data.SortOrder
import com.pixelus.music.data.playlist.LocalPlaylist
import com.pixelus.music.ui.theme.*

data class CombinedPlaylist(
    val id: String,
    val name: String,
    val songCount: Int,
    val isLocal: Boolean = false,
    val localPlaylist: LocalPlaylist? = null
)

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    localPlaylists: List<LocalPlaylist>,
    onPlaylistClick: (Playlist) -> Unit,
    onLocalPlaylistClick: (LocalPlaylist) -> Unit,
    gridView: Boolean = false,
    onRenamePlaylist: (Long, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (Long) -> Unit = {}
) {
    val settings = PixelusApp.settings
    val playlistSort by remember { mutableStateOf(settings.playlistSort) }
    val playlistSortOrder by remember { mutableStateOf(settings.playlistSortOrder) }

    val combined = remember(playlists, localPlaylists, playlistSort, playlistSortOrder) {
        val mapped = playlists.map { CombinedPlaylist(id = "media_${it.id}", name = it.name, songCount = it.songCount, localPlaylist = null) } +
            localPlaylists.map { CombinedPlaylist(id = "local_${it.id}", name = it.name, songCount = it.songCount, isLocal = true, localPlaylist = it) }

        val sorted = when (playlistSort) {
            PlaylistSort.Name -> mapped.sortedBy { it.name.lowercase() }
            PlaylistSort.SongCount -> mapped.sortedBy { it.songCount }
            PlaylistSort.DateCreated -> mapped
        }
        if (playlistSortOrder == SortOrder.DESC) sorted.reversed() else sorted
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<LocalPlaylist?>(null) }
    var renameName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                createName = ""
            },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (createName.isNotBlank()) {
                        PixelusApp.playlistRepository.createPlaylist(createName.trim())
                        createName = ""
                        showCreateDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    createName = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = {
                renameTarget = null
                renameName = ""
            },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = renameName.ifEmpty { renameTarget?.name ?: "" },
                    onValueChange = { renameName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameName.isNotBlank()) {
                        onRenamePlaylist(renameTarget!!.id, renameName.trim())
                        renameTarget = null
                        renameName = ""
                    }
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = {
                    renameTarget = null
                    renameName = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (combined.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No playlists found", color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist")
                }
            }
        }
        return
    }

    if (gridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "add") {
                AddPlaylistCard(onClick = { showCreateDialog = true })
            }
            items(combined, key = { it.id }) { playlist ->
                PlaylistGridItem(
                    playlist = playlist,
                    onClick = {
                        if (playlist.isLocal) onLocalPlaylistClick(playlist.localPlaylist!!)
                    }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item(key = "add") {
                Surface(
                    color = Surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { showCreateDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "New Playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Primary
                        )
                    }
                }
            }
            items(combined, key = { it.id }) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    onClick = {
                        if (playlist.isLocal) onLocalPlaylistClick(playlist.localPlaylist!!)
                    },
                    onRename = {
                        renameTarget = playlist.localPlaylist
                        renameName = playlist.name
                    },
                    onDelete = {
                        if (playlist.isLocal) onDeletePlaylist(playlist.localPlaylist!!.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun AddPlaylistCard(onClick: () -> Unit) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "New Playlist",
                style = MaterialTheme.typography.bodyMedium,
                color = Primary
            )
        }
    }
}

@Composable
private fun PlaylistListItem(
    playlist: CombinedPlaylist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = Surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (playlist.isLocal) Icons.Default.PlaylistAdd else Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (playlist.isLocal) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { showMenu = false; onRename() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistGridItem(
    playlist: CombinedPlaylist,
    onClick: () -> Unit
) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (playlist.isLocal) Icons.Default.PlaylistAdd else Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
