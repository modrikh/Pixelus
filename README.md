# Pixelus Music

A local Android music player with a Lotus-inspired design, built for Pixel devices.

## Features

- **6‑tab library** — Songs, Albums, Artists, Genres, Playlists, Folders
- **Now Playing** — Blurred album art background, seekbar, shuffle/repeat, lyrics (synced & plain text)
- **Lyrics** — Embedded ID3 tags, `.lrc`/`.txt` sidecar files, LRCLIB online fallback
- **Turntable widget** — 1×1 vinyl‑style home screen widget that spins while playing
- **Home screen widgets** — 4 sizes (1×1, 4×1, 5×2, 4×4) with playback controls
- **Sleep timer** — Auto‑pause after 15/30/45/60 minutes
- **Dynamic colors** — Material You on Android 12+, dark Samsung‑style fallback
- **Search** — Real‑time filter by title, artist, or album

## Tech Stack

- **Kotlin** + Jetpack Compose
- **Media3 / ExoPlayer** for playback
- **Media3 MediaSessionService** for background playback & notifications
- **Coil** for album art loading
- **Ktor** for LRCLIB API calls
- **Kotlinx Serialization** for JSON

## Minimum Requirements

- Android 8.0 (API 26) or later
- Local audio files on device storage

## Building

```bash
git clone https://github.com/yourusername/pixelus-music.git
cd pixelus-music
./gradlew assembleDebug
```

## License

GNU General Public License v3.0
