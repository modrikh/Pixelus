package com.pixelus.music.data

data class Lyrics(
    val lines: List<LyricsLine>,
    val isSynced: Boolean
) {
    companion object {
        fun fromPlainText(text: String): Lyrics {
            val lines = text.lines()
                .filter { it.isNotBlank() }
                .map { LyricsLine(timestampMs = -1, text = it.trim()) }
            return Lyrics(lines = lines, isSynced = false)
        }

        fun fromLrc(lrcContent: String): Lyrics? {
            val linePattern = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
            val parsed = lrcContent.lines().mapNotNull { line ->
                linePattern.find(line)?.let { match ->
                    val minutes = match.groupValues[1].toIntOrNull() ?: return@let null
                    val seconds = match.groupValues[2].toIntOrNull() ?: return@let null
                    val hundredths = match.groupValues[3].padEnd(3, '0').take(3).toIntOrNull() ?: return@let null
                    val text = match.groupValues[4].trim()
                    if (text.isBlank()) return@let null
                    val timestampMs = minutes * 60_000L + seconds * 1000L + hundredths
                    LyricsLine(timestampMs = timestampMs, text = text)
                }
            }
            if (parsed.isEmpty()) return null
            return Lyrics(lines = parsed.sortedBy { it.timestampMs }, isSynced = true)
        }

        fun fromEmbedded(metadata: String): Lyrics? {
            if (metadata.isBlank()) return null
            val lrcResult = fromLrc(metadata)
            if (lrcResult != null) return lrcResult
            return fromPlainText(metadata)
        }
    }
}

data class LyricsLine(
    val timestampMs: Long,
    val text: String
)
