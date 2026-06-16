package com.pixelus.music.data.metadata

import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.util.Log
import com.pixelus.music.data.Song
import com.pixelus.music.data.result.DataError
import com.pixelus.music.data.result.Result
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.valuepair.ImageFormats
import org.jaudiotagger.tag.images.AndroidArtwork
import org.jaudiotagger.tag.reference.PictureTypes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MetadataWriterImpl(
    private val context: Context
) : MetadataWriter {
    private val logTag = "MetadataWriter"

    override val unsupportedArtworkEditFormats: List<String>
        get() = listOf("flac", "ogg")

    override fun readMetadata(song: Song): Result<Metadata, DataError> {
        return try {
            val file = copyToTempFile(song) ?: return Result.Error(DataError.FileNotFound)
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagAndConvertOrCreateAndSetDefault
                ?: return Result.Error(DataError.FailedToRead)

            val artwork = tag.artworkList.firstOrNull()
            val coverBytes = artwork?.binaryData

            Result.Success(
                Metadata(
                    title = tag.getFirst(FieldKey.TITLE).ifBlank { null },
                    artist = tag.getFirst(FieldKey.ARTIST).ifBlank { null },
                    album = tag.getFirst(FieldKey.ALBUM).ifBlank { null },
                    albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST).ifBlank { null },
                    genre = tag.getFirst(FieldKey.GENRE).ifBlank { null },
                    year = tag.getFirst(FieldKey.YEAR).ifBlank { null },
                    trackNumber = tag.getFirst(FieldKey.TRACK).ifBlank { null },
                    coverArtBytes = coverBytes,
                    lyrics = tag.getFirst(FieldKey.LYRICS).ifBlank { null }
                )
            )
        } catch (e: Exception) {
            Log.e(logTag, "Failed to read metadata", e)
            Result.Error(DataError.FailedToRead)
        }
    }

    override fun writeMetadata(
        song: Song,
        metadata: Metadata,
        onSecurityError: (IntentSender) -> Unit
    ): Result<Unit, DataError> {
        return try {
            val file = copyToTempFile(song) ?: return Result.Error(DataError.FileNotFound)
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagAndConvertOrCreateAndSetDefault
                ?: return Result.Error(DataError.FailedToRead)

            metadata.title?.let { tag.setField(FieldKey.TITLE, it) }
            metadata.artist?.let { tag.setField(FieldKey.ARTIST, it) }
            metadata.album?.let { tag.setField(FieldKey.ALBUM, it) }
            metadata.albumArtist?.let { tag.setField(FieldKey.ALBUM_ARTIST, it) }
            metadata.genre?.let { tag.setField(FieldKey.GENRE, it) }
            metadata.year?.let { tag.setField(FieldKey.YEAR, it) }
            metadata.trackNumber?.let { tag.setField(FieldKey.TRACK, it) }
            metadata.lyrics?.let { tag.setField(FieldKey.LYRICS, it) }

            metadata.coverArtBytes?.let { bytes ->
                val cover = AndroidArtwork.createArtworkFromFile(file)
                cover.binaryData = bytes
                cover.mimeType = ImageFormats.getMimeTypeForBinarySignature(bytes)
                cover.pictureType = PictureTypes.DEFAULT_ID
                cover.description = ""
                tag.deleteArtworkField()
                tag.setField(cover)
            }

            audioFile.commit()
            writeToOriginal(song, file)

            Result.Success(Unit)
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = e.javaClass.getMethod("getUserAction").invoke(e) as? IntentSender
                if (intentSender != null) {
                    onSecurityError(intentSender)
                    return Result.Error(DataError.NoWritePermission)
                }
            }
            Result.Error(DataError.NoWritePermission)
        } catch (e: Exception) {
            Log.e(logTag, "Failed to write metadata", e)
            Result.Error(DataError.FailedToWrite)
        }
    }

    override fun writeLyricsToTag(song: Song, lyrics: String): Result<Unit, DataError> {
        return writeMetadata(song, Metadata(lyrics = lyrics)) {}
    }

    override fun deleteLyricsFromTag(song: Song): Result<Unit, DataError> {
        return try {
            val file = copyToTempFile(song) ?: return Result.Error(DataError.FileNotFound)
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagAndConvertOrCreateAndSetDefault
                ?: return Result.Error(DataError.FailedToRead)
            tag.deleteField(FieldKey.LYRICS)
            audioFile.commit()
            writeToOriginal(song, file)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(logTag, "Failed to delete lyrics", e)
            Result.Error(DataError.FailedToWrite)
        }
    }

    private fun copyToTempFile(song: Song): File? {
        var file: File? = null
        try {
            context.contentResolver.openInputStream(song.uri)?.use { input ->
                val format = song.uri.lastPathSegment?.substringAfterLast(".") ?: "mp3"
                val temp = File.createTempFile("temp_audio", ".$format", context.cacheDir)
                FileOutputStream(temp).use { output -> input.copyTo(output) }
                file = temp
            }
        } catch (e: Exception) {
            Log.e(logTag, "Failed to copy to temp file", e)
        }
        return file
    }

    private fun writeToOriginal(song: Song, tempFile: File) {
        try {
            context.contentResolver.openOutputStream(song.uri, "wt")?.use { output ->
                FileInputStream(tempFile).use { input -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Log.e(logTag, "Failed to write back to original", e)
        }
    }
}
