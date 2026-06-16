package com.pixelus.music.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelus.music.PixelusApp
import com.pixelus.music.data.Song
import com.pixelus.music.data.metadata.Metadata
import com.pixelus.music.data.result.Result
import com.pixelus.music.data.result.DataError
import com.pixelus.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataSheet(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val metadataWriter = PixelusApp.metadataWriter
    var metadata by remember { mutableStateOf<Metadata?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var showWarning by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var albumArtist by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var trackNumber by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var coverArtBytes by remember { mutableStateOf<ByteArray?>(null) }
    var coverArtBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val coverArtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    coverArtBytes = input.readBytes()
                }
                val bmp = android.graphics.BitmapFactory.decodeByteArray(coverArtBytes, 0, coverArtBytes!!.size)
                if (bmp != null) {
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, 256, 256, true)
                    coverArtBitmap = scaled
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(song) {
        loading = true
        when (val result = metadataWriter.readMetadata(song)) {
            is Result.Success<*> -> {
                val data = result.data as? Metadata ?: return@LaunchedEffect
                metadata = data
                title = data.title ?: song.title
                artist = data.artist ?: song.artist
                album = data.album ?: song.album
                albumArtist = data.albumArtist ?: ""
                genre = data.genre ?: song.genre
                year = data.year ?: if (song.year > 0) song.year.toString() else ""
                trackNumber = data.trackNumber ?: if (song.trackNumber > 0) song.trackNumber.toString() else ""
                lyrics = data.lyrics ?: ""
                data.coverArtBytes?.let { coverArtBytes = it }
            }
            is Result.Error<*> -> {
                errorMessage = "Failed to read metadata"
            }
        }
        loading = false
    }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Primary) },
            title = { Text("Warning: Metadata Editing") },
            text = { Text("Editing metadata modifies the audio file. Make sure you have a backup.") },
            confirmButton = {
                TextButton(onClick = { showWarning = false }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Edit Metadata",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = TextSecondary)
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverArtBitmap != null) {
                        Image(
                            bitmap = coverArtBitmap!!.asImageBitmap(),
                            contentDescription = "Cover Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (coverArtBytes != null) {
                        val bmp = try {
                            android.graphics.BitmapFactory.decodeByteArray(coverArtBytes, 0, coverArtBytes!!.size)
                        } catch (_: Exception) { null }
                        if (bmp != null) {
                            coverArtBitmap = bmp
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Cover Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { coverArtPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick Cover Art")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = albumArtist,
                    onValueChange = { albumArtist = it },
                    label = { Text("Album Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = trackNumber,
                    onValueChange = { trackNumber = it },
                    label = { Text("Track #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Lyrics") },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = TextSecondary, textAlign = TextAlign.Center)
                }

                if (showSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Metadata saved successfully!", color = Primary, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            saving = true
                            errorMessage = null
                            showSuccess = false
                            val newMetadata = Metadata(
                                title = title.ifBlank { null },
                                artist = artist.ifBlank { null },
                                album = album.ifBlank { null },
                                albumArtist = albumArtist.ifBlank { null },
                                genre = genre.ifBlank { null },
                                year = year.ifBlank { null },
                                trackNumber = trackNumber.ifBlank { null },
                                coverArtBytes = coverArtBytes,
                                lyrics = lyrics.ifBlank { null }
                            )
                            when (val result = metadataWriter.writeMetadata(song, newMetadata) {}) {
                                is Result.Success<*> -> {
                                    showSuccess = true
                                }
                                is Result.Error<*> -> {
                                    errorMessage = "Failed to save metadata: ${result.error}"
                                }
                            }
                            saving = false
                        },
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                color = OnPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
