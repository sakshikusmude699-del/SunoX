package com.soundamplifier.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

private val PrimaryPurple = Color(0xFF7C5CBF)
private val PurpleTint = Color(0x1A7C5CBF)

@Composable
fun WaveformView(
    data: ShortArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        val width = size.width
        val height = size.height
        val mid = height / 2
        val path = Path()

        if (data.isEmpty()) return@Canvas
        val step = width / data.size

        path.moveTo(0f, mid)
        for (i in data.indices) {
            val x = i * step
            val normalized = data[i] / 32768f
            val y = mid - (normalized * mid * 0.9f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = PrimaryPurple,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        drawLine(
            color = PurpleTint,
            start = Offset(0f, mid),
            end = Offset(width, mid),
            strokeWidth = 1f
        )
    }
}

@Composable
fun SpectrumView(
    data: ShortArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val barCount = 32
        val barWidth = size.width / barCount - 2.dp.toPx()
        val chunkSize = data.size / barCount

        for (i in 0 until barCount) {
            var sum = 0.0
            for (j in 0 until chunkSize) {
                val idx = i * chunkSize + j
                if (idx < data.size) {
                    val s = data[idx] / 32768f
                    sum += s * s
                }
            }
            val rms = sqrt(sum / chunkSize).toFloat()
            val barHeight = (rms * size.height * 4f).coerceAtMost(size.height)

            val alpha = 0.4f + (i.toFloat() / barCount) * 0.6f
            drawRoundRect(
                color = PrimaryPurple.copy(alpha = alpha),
                topLeft = Offset(i * (barWidth + 2.dp.toPx()), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx().toFloat())
            )
        }
    }
}

@Composable
fun AudioVisualizer(
    waveformData: ShortArray,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "AUDIO OUTPUT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        WaveformView(data = waveformData)
        Spacer(modifier = Modifier.height(8.dp))
        SpectrumView(data = waveformData)
    }
}
