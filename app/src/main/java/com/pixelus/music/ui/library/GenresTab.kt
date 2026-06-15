package com.pixelus.music.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelus.music.data.Genre
import com.pixelus.music.ui.theme.*

@Composable
fun GenresTab(
    genres: List<Genre>,
    onGenreClick: (Genre) -> Unit
) {
    if (genres.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No genres found", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(genres) { genre ->
            GenreItem(genre = genre, onClick = { onGenreClick(genre) })
        }
    }
}

@Composable
private fun GenreItem(
    genre: Genre,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = AccentBlue
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = genre.name,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${genre.songCount} songs",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
    }
}
