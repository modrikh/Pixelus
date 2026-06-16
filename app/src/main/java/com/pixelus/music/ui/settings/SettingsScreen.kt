package com.pixelus.music.ui.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelus.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    sleepTimerActive: Boolean = false,
    sleepTimerRemaining: Long = 0,
    onStartSleepTimer: (Long) -> Unit = {},
    onStopSleepTimer: () -> Unit = {}
) {
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    SleepTimerChip(
                        active = sleepTimerActive,
                        onStart = onStartSleepTimer,
                        onStop = onStopSleepTimer
                    )
                }
            )

            SettingsDivider()

            SectionHeader("Audio")

            SettingsItem(
                icon = Icons.Default.Tune,
                title = "Equalizer",
                subtitle = "Adjust bass, treble, and more",
                onClick = { dialog = SettingsDialog.EQUALIZER }
            )

            SettingsDivider()

            SettingsItem(
                icon = Icons.Default.AudioFile,
                title = "Audio Quality",
                subtitle = "Gapless playback, crossfade",
                onClick = { dialog = SettingsDialog.AUDIO_QUALITY }
            )

            SettingsDivider()

            SectionHeader("Display")

            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = "Dynamic color, dark mode",
                onClick = { dialog = SettingsDialog.THEME }
            )

            SettingsDivider()

            SettingsItem(
                icon = Icons.Default.Lyrics,
                title = "Lyrics",
                subtitle = "Background fetch, sync offset",
                onClick = { dialog = SettingsDialog.LYRICS }
            )

            SettingsDivider()

            SectionHeader("Library")

            SettingsItem(
                icon = Icons.Default.Folder,
                title = "Music Folders",
                subtitle = "Choose which folders to scan",
                onClick = { dialog = SettingsDialog.MUSIC_FOLDERS }
            )

            SettingsDivider()

            SettingsItem(
                icon = Icons.Default.Storage,
                title = "Storage & Cache",
                subtitle = "Manage cached album art",
                onClick = { dialog = SettingsDialog.STORAGE }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Divider(
                color = DividerColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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

    dialog?.let { showDialog(it) { dialog = null } }
}

private enum class SettingsDialog {
    EQUALIZER, AUDIO_QUALITY, THEME, LYRICS, MUSIC_FOLDERS, STORAGE
}

@Composable
private fun showDialog(type: SettingsDialog, onDismiss: () -> Unit) {
    val context = LocalContext.current

    when (type) {
        SettingsDialog.EQUALIZER -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = Primary) },
                title = { Text("Equalizer") },
                text = {
                    Text("Open your device's system equalizer to adjust bass, treble, and other audio effects.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                        onDismiss()
                    }) {
                        Text("Open Equalizer", color = Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                containerColor = Surface,
                titleContentColor = OnSurface,
                textContentColor = TextSecondary
            )
        }

        SettingsDialog.AUDIO_QUALITY -> {
            var gapless by remember { mutableStateOf(true) }
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.AudioFile, contentDescription = null, tint = Primary) },
                title = { Text("Audio Quality") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { gapless = !gapless }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = gapless,
                                onCheckedChange = { gapless = it },
                                colors = CheckboxDefaults.colors(checkedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gapless playback", color = OnSurface)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Done", color = Primary) }
                },
                containerColor = Surface,
                titleContentColor = OnSurface,
                textContentColor = TextSecondary
            )
        }

        SettingsDialog.THEME -> {
            var selected by remember { mutableStateOf(0) }
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Palette, contentDescription = null, tint = Primary) },
                title = { Text("Theme") },
                text = {
                    Column {
                        ThemeOption("Dynamic (Material You)", 0, selected) { selected = it }
                        ThemeOption("Dark", 1, selected) { selected = it }
                        ThemeOption("Light", 2, selected) { selected = it }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Done", color = Primary) }
                },
                containerColor = Surface,
                titleContentColor = OnSurface,
                textContentColor = TextSecondary
            )
        }

        SettingsDialog.LYRICS -> {
            var bgFetch by remember { mutableStateOf(true) }
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Lyrics, contentDescription = null, tint = Primary) },
                title = { Text("Lyrics") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bgFetch = !bgFetch }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = bgFetch,
                                onCheckedChange = { bgFetch = it },
                                colors = CheckboxDefaults.colors(checkedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-fetch from LRCLIB", color = OnSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "When enabled, lyrics are loaded from lrclib.net if not found locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Done", color = Primary) }
                },
                containerColor = Surface,
                titleContentColor = OnSurface,
                textContentColor = TextSecondary
            )
        }

        SettingsDialog.MUSIC_FOLDERS -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Primary) },
                title = { Text("Music Folders") },
                text = {
                    Text("Pixelus Music scans all audio files on your device. Folder filtering will be available in a future update.")
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK", color = Primary) }
                },
                containerColor = Surface,
                titleContentColor = OnSurface,
                textContentColor = TextSecondary
            )
        }

        SettingsDialog.STORAGE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = Primary) },
                title = { Text("Storage & Cache") },
                text = {
                    Text("Album art is loaded on demand and cached by the system. No manual cache management is needed.")
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK", color = Primary) }
                },
                containerColor = Surface,
                titleContentColor = OnSurface,
                textContentColor = TextSecondary
            )
        }
    }
}

@Composable
private fun ThemeOption(label: String, index: Int, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(index) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected == index,
            onClick = { onSelect(index) },
            colors = RadioButtonDefaults.colors(selectedColor = Primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = OnSurface)
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
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
            DropdownMenuItem(
                text = { Text("15 minutes") },
                onClick = { expanded = false; onStart(15 * 60 * 1000L) }
            )
            DropdownMenuItem(
                text = { Text("30 minutes") },
                onClick = { expanded = false; onStart(30 * 60 * 1000L) }
            )
            DropdownMenuItem(
                text = { Text("45 minutes") },
                onClick = { expanded = false; onStart(45 * 60 * 1000L) }
            )
            DropdownMenuItem(
                text = { Text("1 hour") },
                onClick = { expanded = false; onStart(60 * 60 * 1000L) }
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        color = DividerColor,
        modifier = Modifier.padding(start = 72.dp, end = 16.dp)
    )
}
