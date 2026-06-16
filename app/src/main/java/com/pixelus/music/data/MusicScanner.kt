package com.pixelus.music.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicScanner(
    private val context: Context,
    private val settings: PixelusSettings
) {
    private val allowedExtensions = setOf("mp3", "wav", "aac", "flac", "ogg", "m4a")

    suspend fun refreshMedia(onComplete: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                val isScanModeInclusive = settings.isScanModeInclusive.value
                val directoriesToScan = if (isScanModeInclusive) {
                    settings.extraScanFolders.value.map { java.io.File(it) }.toMutableList().apply {
                        if (settings.scanMusicFolder.value) {
                            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
                        }
                    }
                } else {
                    listOf(Environment.getExternalStorageDirectory())
                }
                val excluded = settings.excludedScanFolders.value

                val paths = directoriesToScan.flatMap { dir ->
                    dir.walkTopDown()
                        .onEnter { if (isScanModeInclusive) true else it.absolutePath !in excluded }
                        .filter { it.isFile && it.extension.lowercase() in allowedExtensions }
                        .map { it.absolutePath }
                }.toTypedArray()

                if (paths.isNotEmpty()) {
                    MediaScannerConnection.scanFile(context, paths, arrayOf("audio/*"), null)
                }
            } catch (_: Exception) { }
            onComplete()
        }
    }

    suspend fun scanFolder(path: String, onComplete: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf("audio/*"), null)
            } catch (_: Exception) { }
            onComplete()
        }
    }

    fun findFoldersWithAudio(): Set<String> {
        val isScanModeInclusive = settings.isScanModeInclusive.value
        val directoriesToScan = if (isScanModeInclusive) {
            settings.extraScanFolders.value.map { java.io.File(it) }.toMutableList().apply {
                if (settings.scanMusicFolder.value) {
                    add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
                }
            }
        } else {
            listOf(Environment.getExternalStorageDirectory())
        }

        return directoriesToScan.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in allowedExtensions }
                .map { it.parentFile?.absolutePath ?: "" }
                .distinct()
        }.filter { it.isNotEmpty() }.toSet()
    }
}
