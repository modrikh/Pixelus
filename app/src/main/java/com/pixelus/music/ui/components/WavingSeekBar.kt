package com.pixelus.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pixelus.music.ui.theme.Primary
import com.pixelus.music.ui.theme.SurfaceVariant
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavingSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    var dragProgress by remember { mutableFloatStateOf(progress) }
    val displayProgress = remember(progress, dragProgress) { if (dragProgress != progress) dragProgress else progress }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barCount = 48
    val barWidthFraction = 0.6f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeek((dragProgress * duration).toLong())
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newProgress = dragProgress + (dragAmount / size.width)
                        dragProgress = newProgress.coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val barWidth = size.width / barCount
        val halfHeight = size.height / 2f
        val playedColor = Primary
        val unplayedColor = SurfaceVariant
        val seekIndex = (displayProgress * barCount).toInt().coerceIn(0, barCount - 1)

        for (i in 0 until barCount) {
            val x = i * barWidth + barWidth / 2
            val amplitude = sin(i.toFloat() * 0.4f + phase) * 0.4f + 0.6f
            val barHeight = (amplitude * halfHeight * 0.8f).coerceAtLeast(4f)
            val isPlayed = i <= seekIndex
            val color = if (isPlayed) playedColor else unplayedColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x - (barWidth * barWidthFraction) / 2f, halfHeight - barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth * barWidthFraction, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    }
}
