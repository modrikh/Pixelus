package com.pixelus.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScrollToTopAndLocateButtons(
    showScrollToTopButton: Boolean,
    onScrollToTopClick: () -> Unit,
    showLocateButton: Boolean,
    onLocateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showScrollToTopButton || showLocateButton,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.padding(end = 16.dp, bottom = 80.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showLocateButton) {
                FilledTonalIconButton(onClick = onLocateClick) {
                    Icon(
                        Icons.Rounded.MyLocation,
                        contentDescription = "Locate current track",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (showScrollToTopButton) {
                FilledTonalIconButton(onClick = onScrollToTopClick) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
