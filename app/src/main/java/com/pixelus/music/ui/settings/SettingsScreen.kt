package com.pixelus.music.ui.settings

import androidx.compose.foundation.background
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
                subtitle = "Adjust bass, treble, and more"
            )

            SettingsDivider()

            SettingsItem(
                icon = Icons.Default.AudioFile,
                title = "Audio Quality",
                subtitle = "Gapless playback, crossfade"
            )

            SettingsDivider()

            SectionHeader("Display")

            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = "Dynamic color, dark mode"
            )

            SettingsDivider()

            SettingsItem(
                icon = Icons.Default.Lyrics,
                title = "Lyrics",
                subtitle = "Background fetch, sync offset"
            )

            SettingsDivider()

            SectionHeader("Library")

            SettingsItem(
                icon = Icons.Default.Folder,
                title = "Music Folders",
                subtitle = "Choose which folders to scan"
            )

            SettingsDivider()

            SettingsItem(
                icon = Icons.Default.Storage,
                title = "Storage & Cache",
                subtitle = "Manage cached album art"
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
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
