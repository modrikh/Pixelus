package com.pixelus.music.ui.settings

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.pixelus.music.PixelusApp
import com.pixelus.music.data.Appearance
import com.pixelus.music.data.Song
import com.pixelus.music.player.MusicService
import com.pixelus.music.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPage {
    MAIN, PLAYBACK, MUSIC_SCAN, THEME, LYRICS, PLAYLISTS, ABOUT
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    sleepTimerActive: Boolean = false,
    sleepTimerRemaining: Long = 0,
    onStartSleepTimer: (Long) -> Unit = {},
    onStopSleepTimer: () -> Unit = {},
    songs: List<Song> = emptyList()
) {
    val context = LocalContext.current
    val settings = remember { PixelusApp.settings }
    val musicScanner = remember { PixelusApp.musicScanner }
    val equalizerController = remember { MusicService.equalizerController }

    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            if (targetState == SettingsPage.MAIN) {
                slideInHorizontally { -it / 5 } + fadeIn() togetherWith
                        slideOutHorizontally { it / 5 } + fadeOut()
            } else {
                slideInHorizontally { it / 5 } + fadeIn() togetherWith
                        slideOutHorizontally { -it / 5 } + fadeOut()
            }
        },
        label = "settings-nav"
    ) { page ->
        when (page) {
            SettingsPage.MAIN -> MainSettingsPage(
                sleepTimerActive = sleepTimerActive,
                sleepTimerRemaining = sleepTimerRemaining,
                onStartSleepTimer = onStartSleepTimer,
                onStopSleepTimer = onStopSleepTimer,
                onNavigate = { currentPage = it },
                onBack = onBack
            )
            SettingsPage.PLAYBACK -> PlaybackSettingsPage(
                settings = settings,
                equalizerController = equalizerController,
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.MUSIC_SCAN -> MusicScanSettingsPage(
                settings = settings,
                musicScanner = musicScanner,
                context = context,
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.THEME -> ThemeSettingsPage(
                settings = settings,
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.LYRICS -> LyricsSettingsPage(
                settings = settings,
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.PLAYLISTS -> PlaylistsSettingsPage(
                settings = settings,
                songs = songs,
                context = context,
                onBack = { currentPage = SettingsPage.MAIN }
            )
            SettingsPage.ABOUT -> AboutPage(
                onBack = { currentPage = SettingsPage.MAIN }
            )
        }
    }
}

// ── MAIN SETTINGS PAGE ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsPage(
    sleepTimerActive: Boolean,
    sleepTimerRemaining: Long,
    onStartSleepTimer: (Long) -> Unit,
    onStopSleepTimer: () -> Unit,
    onNavigate: (SettingsPage) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("Playback")
            SettingsItem(
                icon = Icons.Default.Timer,
                title = "Sleep Timer",
                subtitle = if (sleepTimerActive) "${sleepTimerRemaining / 60000} min remaining"
                          else "Set a timer to pause playback",
                trailing = {
                    SleepTimerChip(sleepTimerActive, onStartSleepTimer, onStopSleepTimer)
                }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.MusicNote,
                title = "Playback",
                subtitle = "Audio focus, equalizer, gapless",
                onClick = { onNavigate(SettingsPage.PLAYBACK) }
            )

            SectionHeader("Library")
            SettingsItem(
                icon = Icons.Default.Radar,
                title = "Music Scan",
                subtitle = "Scan folders, filter options",
                onClick = { onNavigate(SettingsPage.MUSIC_SCAN) }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Playlists",
                subtitle = "Import, grid view",
                onClick = { onNavigate(SettingsPage.PLAYLISTS) }
            )

            SectionHeader("Display")
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = "Appearance, colors, AMOLED mode",
                onClick = { onNavigate(SettingsPage.THEME) }
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Default.Lyrics,
                title = "Lyrics",
                subtitle = "Auto-fetch, display options",
                onClick = { onNavigate(SettingsPage.LYRICS) }
            )

            SectionHeader("Info")
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Version, source code, feedback",
                onClick = { onNavigate(SettingsPage.ABOUT) }
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Pixelus Music v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

// ── PLAYBACK SETTINGS ──────────────────────────────────────────────

@Composable
private fun PlaybackSettingsPage(
    settings: com.pixelus.music.data.PixelusSettings,
    equalizerController: com.pixelus.music.player.EqualizerController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SettingsSubPage(
        title = "Playback",
        onBack = onBack
    ) {
        var audioFocus by remember { mutableStateOf(settings.handleAudioFocus) }
        SettingSwitch(
            icon = Icons.Default.FilterCenterFocus,
            title = "Audio Focus",
            subtitle = "Transiently duck other audio when music plays",
            checked = audioFocus,
            onCheckedChange = {
                settings.handleAudioFocus = it
                audioFocus = it
            }
        )

        ExternalSpacer()

        var jumpToBeginning by remember { mutableStateOf(settings.jumpToBeginning) }
        SettingSwitch(
            icon = Icons.Default.SkipPrevious,
            title = "Jump to Beginning",
            subtitle = "Skip to previous track start when pressing prev within 3s",
            checked = jumpToBeginning,
            onCheckedChange = {
                settings.jumpToBeginning = it
                jumpToBeginning = it
            }
        )

        ExternalSpacer()

        val isEqEnabled by equalizerController.isEqEnabled.collectAsState()
        SettingSwitch(
            icon = Icons.Default.Equalizer,
            title = "Equalizer",
            subtitle = "Adjust frequency bands for audio output",
            checked = isEqEnabled,
            onCheckedChange = equalizerController::updateIsEqEnabled
        )

        AnimatedVisibility(visible = isEqEnabled) {
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                val bandFreqs by equalizerController.bandFrequencies.collectAsState()
                val bandLevels by equalizerController.bandLevels.collectAsState()
                val lowerLimit by equalizerController.lowerLevelLimit.collectAsState()
                val upperLimit by equalizerController.upperLevelLimit.collectAsState()

                bandLevels?.forEachIndexed { index, level ->
                    val freq = bandFreqs?.getOrNull(index) ?: "Band $index"
                    val displayValue = "${level / 100f} dB"
                    SettingSlider(
                        title = freq,
                        value = level.toFloat(),
                        valueDisplay = displayValue,
                        valueRange = lowerLimit.toFloat()..upperLimit.toFloat(),
                        onValueChange = { newVal ->
                            bandLevels?.let { levels ->
                                equalizerController.updateBandLevels(
                                    levels.toMutableList().apply { set(index, newVal.toInt().toShort()) }
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = equalizerController::resetBandLevels,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

// ── MUSIC SCAN SETTINGS ────────────────────────────────────────────

@Composable
private fun MusicScanSettingsPage(
    settings: com.pixelus.music.data.PixelusSettings,
    musicScanner: com.pixelus.music.data.MusicScanner,
    context: android.content.Context,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var foldersWithAudio by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showScanDialog by remember { mutableStateOf(false) }

    SettingsSubPage(
        title = "Music Scan",
        onBack = onBack
    ) {
        val isInclusive by settings.isScanModeInclusive.collectAsState()
        SettingSegmentOptions(
            icon = Icons.Default.Search,
            title = "Filter Mode",
            subtitle = "Inclusive: scan selected folders only. Exclusive: scan everything except excluded.",
            selectedIndex = if (isInclusive) 0 else 1,
            options = listOf(
                "Inclusive", "Exclusive"
            ),
            onSelect = { settings.updateIsScanModeInclusive(it == 0) }
        )

        ExternalSpacer()

        var ignoreShort by remember { mutableStateOf(settings.ignoreShortTracks) }
        SettingSwitch(
            icon = Icons.Default.Timelapse,
            title = "Ignore Short Tracks",
            subtitle = "Exclude tracks shorter than 30 seconds",
            checked = ignoreShort,
            onCheckedChange = {
                settings.ignoreShortTracks = it
                ignoreShort = it
            }
        )

        ExternalSpacer()

        if (isInclusive) {
            val scanMusicFolder by settings.scanMusicFolder.collectAsState()
            SettingSwitch(
                icon = Icons.Default.LibraryMusic,
                title = "Include Music Folder",
                subtitle = "Scan the system ${Environment.DIRECTORY_MUSIC} folder",
                checked = scanMusicFolder,
                onCheckedChange = settings::updateScanMusicFolder
            )
            Spacer(modifier = Modifier.height(4.dp))

            val extraFolders by settings.extraScanFolders.collectAsState()
            SettingsFolderList(
                title = "Included Folders",
                paths = extraFolders.toList(),
                onPickFolder = { showScanDialog = true },
                onRemoveFolder = { settings.updateExtraScanFolders(extraFolders - it) },
                availableOptions = foldersWithAudio.toList(),
                onAddFolderFromOptions = { settings.updateExtraScanFolders(extraFolders + it) },
                onScanFolders = { foldersWithAudio = musicScanner.findFoldersWithAudio() }
            )
        } else {
            val excludedFolders by settings.excludedScanFolders.collectAsState()
            SettingsFolderList(
                title = "Excluded Folders",
                paths = excludedFolders.toList(),
                onPickFolder = { showScanDialog = true },
                onRemoveFolder = { settings.updateExcludedScanFolders(excludedFolders - it) },
                availableOptions = foldersWithAudio.toList(),
                onAddFolderFromOptions = { settings.updateExcludedScanFolders(excludedFolders + it) },
                onScanFolders = { foldersWithAudio = musicScanner.findFoldersWithAudio() }
            )
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        val scanOnLaunch by settings.scanOnAppLaunch.collectAsState()
        SettingSwitch(
            icon = Icons.Default.Refresh,
            title = "Refresh on App Launch",
            subtitle = "Rescan audio files when the app starts",
            checked = scanOnLaunch,
            onCheckedChange = settings::updateScanOnAppLaunch
        )

        ExternalSpacer()

        SettingIconButton(
            icon = Icons.Default.Storage,
            title = "Refresh Now",
            subtitle = "Trigger MediaStore to rescan all configured folders",
            buttonIcon = Icons.Default.Refresh,
            onButtonClick = { scope.launch { musicScanner.refreshMedia() } }
        )

        SettingIconButton(
            icon = Icons.Default.FolderOpen,
            title = "Scan Folder",
            subtitle = "Scan a specific folder for audio files",
            buttonIcon = Icons.Default.Radar,
            onButtonClick = { showScanDialog = true }
        )

        if (showScanDialog) {
            AlertDialog(
                onDismissRequest = { showScanDialog = false },
                title = { Text("Scan Folder") },
                text = { Text("Folder scanning uses system picker or type a path.\nCurrently scans all configured folders via MediaStore.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            musicScanner.refreshMedia()
                        }
                        showScanDialog = false
                    }) { Text("Scan Now") }
                },
                dismissButton = {
                    TextButton(onClick = { showScanDialog = false }) { Text("Cancel") }
                },
                containerColor = Surface,
                titleContentColor = OnSurface,
                textContentColor = TextSecondary
            )
        }
    }
}

// ── THEME SETTINGS ─────────────────────────────────────────────────

@Composable
private fun ThemeSettingsPage(
    settings: com.pixelus.music.data.PixelusSettings,
    onBack: () -> Unit
) {
    SettingsSubPage(
        title = "Theme",
        onBack = onBack
    ) {
        val selectedAppearance by settings.appearance.collectAsState()
        SettingOptionsRow(
            title = "Appearance",
            options = listOf(
                "System" to Icons.Default.Settings,
                "Light" to Icons.Default.LightMode,
                "Dark" to Icons.Default.DarkMode
            ),
            selectedIndex = selectedAppearance.ordinal,
            onSelect = { settings.updateAppearance(Appearance.entries[it]) }
        )

        ExternalSpacer()

        val useDynamic by settings.useDynamicColor.collectAsState()
        SettingSwitch(
            icon = Icons.Default.AutoAwesome,
            title = "Use System Key Colors",
            subtitle = "Follow Material You dynamic color scheme (Android 12+)",
            checked = useDynamic,
            onCheckedChange = settings::updateUseDynamicColor
        )

        ExternalSpacer()

        val useAlbumArt by settings.useAlbumArtColor.collectAsState()
        SettingSwitch(
            icon = Icons.Default.Album,
            title = "Use Album Art Colors",
            subtitle = "Theme accent adapts to the currently playing album art",
            checked = useAlbumArt,
            onCheckedChange = settings::updateUseAlbumArtColor
        )

        ExternalSpacer()

        val amoled by settings.amoledDarkTheme.collectAsState()
        SettingSwitch(
            icon = Icons.Default.Contrast,
            title = "AMOLED Dark Theme",
            subtitle = "Use pure black backgrounds to save battery on OLED screens",
            checked = amoled,
            onCheckedChange = settings::updateAmoledDarkTheme
        )
    }
}

// ── LYRICS SETTINGS ────────────────────────────────────────────────

@Composable
private fun LyricsSettingsPage(
    settings: com.pixelus.music.data.PixelusSettings,
    onBack: () -> Unit
) {
    SettingsSubPage(
        title = "Lyrics",
        onBack = onBack
    ) {
        var autoFetch by remember { mutableStateOf(settings.lyricsAutoFetch) }
        SettingSwitch(
            icon = Icons.Default.Lyrics,
            title = "Auto-fetch from LRCLIB",
            subtitle = "When enabled, lyrics are loaded from lrclib.net if not found locally in tags or sidecar files",
            checked = autoFetch,
            onCheckedChange = {
                settings.lyricsAutoFetch = it
                autoFetch = it
            }
        )
    }
}

// ── PLAYLISTS SETTINGS ─────────────────────────────────────────────

@Composable
private fun PlaylistsSettingsPage(
    settings: com.pixelus.music.data.PixelusSettings,
    songs: List<Song>,
    context: android.content.Context,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var importMessage by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importMessage = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()

                val matched = withContext(Dispatchers.IO) {
                    parseM3u(content, songs)
                }

                if (matched.isEmpty()) {
                    importMessage = "No matching songs found in library"
                    return@launch
                }

                val name = uri.lastPathSegment?.substringBeforeLast(".") ?: "Imported Playlist"
                val playlistUri = createMediaStorePlaylist(context, name, matched)
                if (playlistUri != null) {
                    importMessage = "Imported ${matched.size} songs as '$name'"
                } else {
                    importMessage = "Failed to create playlist"
                }
            } catch (e: Exception) {
                importMessage = "Error: ${e.message}"
            }
        }
    }

    SettingsSubPage(
        title = "Playlists",
        onBack = onBack
    ) {
        val gridPlaylists by settings.gridPlaylists.collectAsState()
        SettingSwitch(
            icon = Icons.Default.GridView,
            title = "Grid View",
            subtitle = "Show playlists as cards instead of a list",
            checked = gridPlaylists,
            onCheckedChange = settings::updateGridPlaylists
        )

        ExternalSpacer()

        SettingIconButton(
            icon = Icons.Default.FileUpload,
            title = "Import Playlist",
            subtitle = "Import an M3U playlist file from storage",
            buttonIcon = Icons.Default.FolderOpen,
            onButtonClick = {
                importLauncher.launch(arrayOf("*/*"))
            }
        )

        if (importMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = importMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (importMessage!!.startsWith("Error") || importMessage!!.startsWith("No"))
                        MaterialTheme.colorScheme.error else Primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private suspend fun parseM3u(content: String, librarySongs: List<Song>): List<Song> = withContext(Dispatchers.IO) {
    val lines = content.lines().filter { it.isNotBlank() && !it.startsWith("#EXTM3U") }
    val parsed = mutableListOf<Pair<String, String>>() // (title, artist)

    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("#EXTINF:")) {
            val meta = line.removePrefix("#EXTINF:")
            val dashIndex = meta.lastIndexOf("-")
            val titlePart = if (dashIndex >= 0) meta.substring(dashIndex + 1).trim() else meta.trim()
            val artistPart = if (dashIndex >= 0) meta.substring(0, dashIndex).trim().substringAfterLast(",").trim() else ""
            parsed.add(titlePart to artistPart)
            i++
        } else if (!line.startsWith("#")) {
            val path = line
            val filename = path.substringAfterLast("/").substringBeforeLast(".")
            parsed.add(filename to "")
            i++
        } else {
            i++
        }
    }

    if (parsed.isEmpty()) return@withContext emptyList()

    val matched = mutableSetOf<Song>()
    for ((titleOrFile, artist) in parsed) {
        val song = librarySongs.firstOrNull { s ->
            val titleMatch = s.title.equals(titleOrFile, ignoreCase = true) ||
                             s.title.contains(titleOrFile, ignoreCase = true) ||
                             titleOrFile.contains(s.title, ignoreCase = true)
            val artistMatch = artist.isBlank() || s.artist.equals(artist, ignoreCase = true) ||
                             s.artist.contains(artist, ignoreCase = true)
            titleMatch && artistMatch
        }
        if (song != null) matched.add(song)
    }
    matched.toList()
}

private suspend fun createMediaStorePlaylist(
    context: android.content.Context,
    name: String,
    songs: List<Song>
): Uri? = withContext(Dispatchers.IO) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Playlists.NAME, name)
        }
        val playlistUri = context.contentResolver.insert(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return@withContext null

        for (i in songs.indices) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i)
                put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs[i].id)
            }
            context.contentResolver.insert(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistUri.lastPathSegment!!.toLong()),
                values
            )
        }
        playlistUri
    } catch (_: Exception) {
        null
    }
}

// ── ABOUT PAGE ─────────────────────────────────────────────────────

@Composable
private fun AboutPage(onBack: () -> Unit) {
    SettingsSubPage(
        title = "About",
        onBack = onBack
    ) {
        Text(
            text = "Pixelus Music",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "A local-first Android music player with Lotus-inspired design.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Open Source",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnBackground,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Text(
            text = "Licensed under GPL-3.0. Source code available at github.com/modrikh/Pixelus",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ── SHARED COMPONENTS ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    valueDisplay: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = OnBackground)
            Text(
                valueDisplay ?: value.toInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            steps = steps,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
        FilledTonalIconButton(onClick = onButtonClick) {
            Icon(buttonIcon, contentDescription = title)
        }
    }
}

@Composable
private fun SettingOptionsRow(
    title: String,
    options: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEachIndexed { index, (label, icon) ->
                val isSelected = index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(index) }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Primary else SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (isSelected) OnPrimary else OnBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Primary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSegmentOptions(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceVariant)
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) OnPrimary else TextSecondary,
                    modifier = Modifier
                        .background(
                            if (isSelected) Primary else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsFolderList(
    title: String,
    paths: List<String>,
    onPickFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    availableOptions: List<String>,
    onAddFolderFromOptions: (String) -> Unit,
    onScanFolders: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                modifier = Modifier.padding(start = 4.dp)
            )
            IconButton(onClick = onPickFolder) {
                Icon(Icons.Default.Add, contentDescription = "Add folder", tint = OnBackground)
            }
        }
        paths.forEach { path ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = TextSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            path.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground
                        )
                        Text(
                            path,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = { onRemoveFolder(path) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Remove", tint = TextSecondary)
                }
            }
        }
        if (availableOptions.isNotEmpty()) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Folders with Audio",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
                IconButton(onClick = onScanFolders) {
                    Icon(Icons.Default.Radar, contentDescription = "Scan", tint = OnBackground)
                }
            }
            availableOptions.filter { it !in paths }.forEach { path ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                path.substringAfterLast('/'),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnBackground
                            )
                            Text(
                                path,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = { onAddFolderFromOptions(path) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalSpacer() {
    Divider(
        color = DividerColor,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsDivider() {
    Divider(
        color = DividerColor,
        modifier = Modifier.padding(start = 72.dp, end = 16.dp)
    )
}

@Composable
private fun SleepTimerChip(
    active: Boolean,
    onStart: (Long) -> Unit,
    onStop: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    if (active) {
        AssistChip(
            onClick = onStop,
            label = { Text("Stop", fontSize = 12.sp) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Primary,
                labelColor = OnPrimary
            ),
            modifier = Modifier.height(28.dp)
        )
    } else {
        AssistChip(
            onClick = { expanded = true },
            label = { Text("Set", fontSize = 12.sp) },
            modifier = Modifier.height(28.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(15L to "15 min", 30L to "30 min", 45L to "45 min", 60L to "1 hour").forEach { (mins, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { expanded = false; onStart(mins * 60 * 1000L) }
                )
            }
        }
    }
}
