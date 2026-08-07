package com.sharik.dbmeter.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.sharik.dbmeter.ui.theme.DbTheme

private const val Y_MAX = 120f
private const val WINDOW_SECONDS = 15f
private val Y_GRID_LINES = listOf(0f, 20f, 40f, 60f, 80f, 100f, 120f)
private val X_GRID_LABELS = listOf(5f, 10f, 15f)

/**
 * [points] are (elapsedSeconds, dbValue) pairs, oldest first, within the
 * current rolling [WINDOW_SECONDS]-second window.
 */
@Composable
fun DbChart(
    points: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    val gridColor = DbTheme.colors.chartGridLine
    val labelColor = DbTheme.colors.textSecondary.toArgb()
    val greenArgb = DbTheme.colors.chartLineStart
    val purpleArgb = DbTheme.colors.chartLineEnd

    // Fills whatever the caller gives it: the chart is the part of the screen
    // that flexes, so the controls below it always keep their full height.
    Canvas(modifier = modifier.fillMaxSize()) {
        val leftPadding = size.width * 0.09f
        val bottomPadding = size.height * 0.1f
        val plotWidth = size.width - leftPadding
        val plotHeight = size.height - bottomPadding

        val labelPaint = Paint().apply {
            color = labelColor
            textSize = size.width * 0.032f
            isAntiAlias = true
        }

        // horizontal grid lines + y labels
        Y_GRID_LINES.forEach { yValue ->
            val y = plotHeight - (yValue / Y_MAX) * plotHeight
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
            labelPaint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(
                yValue.toInt().toString(),
                leftPadding - size.width * 0.02f,
                y + labelPaint.textSize / 3,
                labelPaint
            )
        }

        // x labels
        labelPaint.textAlign = Paint.Align.CENTER
        X_GRID_LABELS.forEach { xValue ->
            val x = leftPadding + (xValue / WINDOW_SECONDS) * plotWidth
            drawContext.canvas.nativeCanvas.drawText(
                xValue.toInt().toString(),
                x,
                size.height - bottomPadding * 0.15f,
                labelPaint
            )
        }

        if (points.size < 2) return@Canvas

        val oldestTime = points.first().first
        val path = Path()
        points.forEachIndexed { index, (t, db) ->
            val xFraction = ((t - oldestTime) / WINDOW_SECONDS).coerceIn(0f, 1f)
            val x = leftPadding + xFraction * plotWidth
            val y = plotHeight - (db / Y_MAX).coerceIn(0f, 1f) * plotHeight
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(greenArgb, greenArgb, purpleArgb),
                startX = leftPadding,
                endX = size.width
            ),
            style = Stroke(
                width = size.width * 0.012f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
