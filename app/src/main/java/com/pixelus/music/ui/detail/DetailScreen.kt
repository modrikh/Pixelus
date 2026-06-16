package com.pixelus.music.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelus.music.data.Song
import com.pixelus.music.ui.theme.*
import com.pixelus.music.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    title: String,
    subtitle: String? = null,
    songs: List<Song>,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground
                )
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (subtitle != null) {
                item {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall)
                    )
                }
            }

            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.spacingExtraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconSizeLarge),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
                            Text("No songs found", color = TextSecondary)
                        }
                    }
                }
            }

            items(songs) { song ->
                SongRow(song = song, onClick = { onSongClick(song, songs) })
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(Dimens.cornerRadiusMedium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spacingSmall + 4.dp, vertical = Dimens.spacingExtraSmall)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingSmall + 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artUri = song.albumArtUri
            if (artUri != null) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Dimens.albumArtMedium - 8.dp)
                        .clip(RoundedCornerShape(Dimens.cornerRadiusSmall)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.albumArtMedium - 8.dp),
                    tint = TextSecondary
                )
            }

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
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
