package com.pixelus.music.data

import android.content.Context

enum class SongSort(val label: String) {
    TITLE("Name"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Date"),
    YEAR("Year");

    companion object {
        fun fromString(value: String): SongSort =
            entries.firstOrNull { it.name == value } ?: TITLE
    }
}

enum class SortOrder(val label: String) {
    ASC("A-Z"),
    DESC("Z-A");

    companion object {
        fun fromBoolean(ascending: Boolean): SortOrder =
            if (ascending) ASC else DESC
    }
}
