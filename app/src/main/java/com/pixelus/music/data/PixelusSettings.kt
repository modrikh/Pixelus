package com.pixelus.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Appearance { System, Light, Dark }

class PixelusSettings(context: Context) {
    private val prefs = context.getSharedPreferences("pixelus_settings", Context.MODE_PRIVATE)

    // Playback
    private val handleAudioFocusKey = "audio_focus"
    private val jumpToBeginningKey = "jump_to_beginning"

    // Music Scan
    private val isScanModeInclusiveKey = "scan_mode_inclusive"
    private val ignoreShortTracksKey = "ignore_short_tracks"
    private val scanMusicFolderKey = "scan_music_folder"
    private val extraScanFoldersKey = "extra_scan_folders"
    private val excludedScanFoldersKey = "excluded_scan_folders"
    private val scanOnAppLaunchKey = "scan_on_app_launch"

    // Theme
    private val appearanceKey = "appearance"
    private val useDynamicColorKey = "use_dynamic_color"
    private val useAlbumArtColorKey = "use_album_art_color"
    private val amoledDarkThemeKey = "amoled_dark_theme"

    // Lyrics
    private val lyricsAutoFetchKey = "lyrics_auto_fetch"

    // Playlists
    private val gridPlaylistsKey = "grid_playlists"

    // Sorting
    private val trackSortKey = "track_sort"
    private val trackSortOrderKey = "track_sort_order"

    private val _trackSort = MutableStateFlow(
        SongSort.fromString(prefs.getString(trackSortKey, SongSort.TITLE.name) ?: SongSort.TITLE.name)
    )
    val trackSort = _trackSort.asStateFlow()
    fun updateTrackSort(value: SongSort) {
        _trackSort.update { value }
        prefs.edit().putString(trackSortKey, value.name).apply()
    }

    private val _trackSortAscending = MutableStateFlow(prefs.getBoolean(trackSortOrderKey, true))
    val trackSortAscending = _trackSortAscending.asStateFlow()
    fun updateTrackSortAscending(value: Boolean) {
        _trackSortAscending.update { value }
        prefs.edit().putBoolean(trackSortOrderKey, value).apply()
    }

    // --- Playback ---

    var handleAudioFocus: Boolean
        get() = prefs.getBoolean(handleAudioFocusKey, true)
        set(value) { prefs.edit().putBoolean(handleAudioFocusKey, value).apply() }

    var jumpToBeginning: Boolean
        get() = prefs.getBoolean(jumpToBeginningKey, true)
        set(value) { prefs.edit().putBoolean(jumpToBeginningKey, value).apply() }

    // --- Music Scan ---

    private val _isScanModeInclusive = MutableStateFlow(prefs.getBoolean(isScanModeInclusiveKey, true))
    val isScanModeInclusive = _isScanModeInclusive.asStateFlow()
    fun updateIsScanModeInclusive(value: Boolean) {
        _isScanModeInclusive.update { value }
        prefs.edit().putBoolean(isScanModeInclusiveKey, value).apply()
    }

    var ignoreShortTracks: Boolean
        get() = prefs.getBoolean(ignoreShortTracksKey, true)
        set(value) { prefs.edit().putBoolean(ignoreShortTracksKey, value).apply() }

    private val _scanMusicFolder = MutableStateFlow(prefs.getBoolean(scanMusicFolderKey, true))
    val scanMusicFolder = _scanMusicFolder.asStateFlow()
    fun updateScanMusicFolder(value: Boolean) {
        _scanMusicFolder.update { value }
        prefs.edit().putBoolean(scanMusicFolderKey, value).apply()
    }

    private val _extraScanFolders = MutableStateFlow(
        prefs.getStringSet(extraScanFoldersKey, emptySet()) ?: emptySet()
    )
    val extraScanFolders = _extraScanFolders.asStateFlow()
    fun updateExtraScanFolders(value: Set<String>) {
        _extraScanFolders.update { value }
        prefs.edit().putStringSet(extraScanFoldersKey, value).apply()
    }

    private val _excludedScanFolders = MutableStateFlow(
        prefs.getStringSet(excludedScanFoldersKey, emptySet()) ?: emptySet()
    )
    val excludedScanFolders = _excludedScanFolders.asStateFlow()
    fun updateExcludedScanFolders(value: Set<String>) {
        _excludedScanFolders.update { value }
        prefs.edit().putStringSet(excludedScanFoldersKey, value).apply()
    }

    private val _scanOnAppLaunch = MutableStateFlow(prefs.getBoolean(scanOnAppLaunchKey, false))
    val scanOnAppLaunch = _scanOnAppLaunch.asStateFlow()
    fun updateScanOnAppLaunch(value: Boolean) {
        _scanOnAppLaunch.update { value }
        prefs.edit().putBoolean(scanOnAppLaunchKey, value).apply()
    }

    // --- Theme ---

    private val _appearance = MutableStateFlow(
        Appearance.entries[prefs.getInt(appearanceKey, 0)]
    )
    val appearance = _appearance.asStateFlow()
    fun updateAppearance(value: Appearance) {
        _appearance.update { value }
        prefs.edit().putInt(appearanceKey, value.ordinal).apply()
    }

    private val _useDynamicColor = MutableStateFlow(prefs.getBoolean(useDynamicColorKey, true))
    val useDynamicColor = _useDynamicColor.asStateFlow()
    fun updateUseDynamicColor(value: Boolean) {
        _useDynamicColor.update { value }
        prefs.edit().putBoolean(useDynamicColorKey, value).apply()
    }

    private val _useAlbumArtColor = MutableStateFlow(prefs.getBoolean(useAlbumArtColorKey, true))
    val useAlbumArtColor = _useAlbumArtColor.asStateFlow()
    fun updateUseAlbumArtColor(value: Boolean) {
        _useAlbumArtColor.update { value }
        prefs.edit().putBoolean(useAlbumArtColorKey, value).apply()
    }

    private val _amoledDarkTheme = MutableStateFlow(prefs.getBoolean(amoledDarkThemeKey, false))
    val amoledDarkTheme = _amoledDarkTheme.asStateFlow()
    fun updateAmoledDarkTheme(value: Boolean) {
        _amoledDarkTheme.update { value }
        prefs.edit().putBoolean(amoledDarkThemeKey, value).apply()
    }

    // --- Lyrics ---

    var lyricsAutoFetch: Boolean
        get() = prefs.getBoolean(lyricsAutoFetchKey, true)
        set(value) { prefs.edit().putBoolean(lyricsAutoFetchKey, value).apply() }

    // --- Playlists ---

    private val _gridPlaylists = MutableStateFlow(prefs.getBoolean(gridPlaylistsKey, true))
    val gridPlaylists = _gridPlaylists.asStateFlow()
    fun updateGridPlaylists(value: Boolean) {
        _gridPlaylists.update { value }
        prefs.edit().putBoolean(gridPlaylistsKey, value).apply()
    }
}
