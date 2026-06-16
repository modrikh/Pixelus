package com.pixelus.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

@Composable
fun CollapsibleTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    collapseFraction: Float,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(targetValue = collapseFraction, label = "collapse")

    Text(
        text = title,
        fontSize = lerp(
            MaterialTheme.typography.titleLarge.fontSize,
            MaterialTheme.typography.displaySmall.fontSize,
            animatedFraction
        ),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
fun ColumnWithCollapsibleTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 28.dp),
    contentHorizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    var collapseFraction by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 8.dp)
    ) {
        CollapsibleTopBar(
            title = title,
            onBackClick = onBackClick,
            collapseFraction = collapseFraction,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            contentAlignment = contentHorizontalAlignment
        ) {
            content()
        }
    }
}
