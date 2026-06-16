package com.pixelus.music.ui.navigation

import com.pixelus.music.data.Song

sealed class Screen {
    data object Library : Screen()
    data object NowPlaying : Screen()
    data object Search : Screen()
    data object Settings : Screen()
    data class Detail(
        val title: String,
        val subtitle: String?,
        val songs: List<Song>
    ) : Screen()
}
