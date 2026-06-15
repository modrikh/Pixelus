package com.pixelus.music.ui.navigation

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object Search : Screen("search")
}
