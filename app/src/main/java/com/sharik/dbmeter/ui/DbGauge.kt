package com.sharik.dbmeter.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.sharik.dbmeter.ui.theme.DbTheme
import com.sharik.dbmeter.ui.theme.LevelDanger
import com.sharik.dbmeter.ui.theme.LevelLoud
import com.sharik.dbmeter.ui.theme.LevelModerate
import com.sharik.dbmeter.ui.theme.LevelQuiet
import com.sharik.dbmeter.ui.theme.LevelVeryLoud
import kotlin.math.cos
import kotlin.math.sin

private const val MIN_VALUE = 0f
private const val MAX_VALUE = 120f
private const val MAJOR_STEP = 20f
private const val MINOR_STEP = 4f

// The level stripe is drawn as a run of short arcs, each a solid colour, so the
// ramp is smooth without a sweep gradient's seam at the 3 o'clock wrap point.
private const val STRIPE_SEGMENTS = 96

private val LevelRamp = listOf(
    0.00f to LevelQuiet,
    0.35f to LevelModerate,
    0.55f to LevelLoud,
    0.75f to LevelVeryLoud,
    1.00f to LevelDanger
)

/** Colour at [fraction] (0..1) along the gauge, ramping green -> red. */
fun levelColor(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    for (i in 0 until LevelRamp.lastIndex) {
        val (start, startColor) = LevelRamp[i]
        val (end, endColor) = LevelRamp[i + 1]
        if (f <= end) return lerp(startColor, endColor, (f - start) / (end - start))
    }
    return LevelRamp.last().second
}

@Composable
fun DbGauge(
    value: Float,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(MIN_VALUE, MAX_VALUE),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 60f),
        label = "needle"
    )

    val trackColor = DbTheme.colors.gaugeTrack
    val minorTickColor = DbTheme.colors.gaugeTrackMinor
    val labelColor = DbTheme.colors.textSecondary.toArgb()
    val hubColor = DbTheme.colors.needleHub

    // The arc geometry is derived from the canvas width, and the 1.75 ratio is
    // the flattest the top semicircle fits in, so shrink the gauge by giving it
    // less width rather than by squashing the aspect ratio.
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.75f)
        ) {
            val w = size.width
            val h = size.height
            val strokeWidth = w * 0.028f
            val outerRadius = (w / 2f) - strokeWidth
            val center = Offset(w / 2f, h * 0.86f)

            val arcTopLeft = Offset(center.x - outerRadius, center.y - outerRadius)
            val arcSize = Size(outerRadius * 2, outerRadius * 2)

            // background track arc (top semicircle)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // level stripe: fills in proportion to the reading, green -> red
            val level = (animatedValue - MIN_VALUE) / (MAX_VALUE - MIN_VALUE)
            if (level > 0f) {
                val segments = (level * STRIPE_SEGMENTS).toInt().coerceAtLeast(1)
                val segmentSweep = (level * 180f) / segments
                for (i in 0 until segments) {
                    val isLast = i == segments - 1
                    drawArc(
                        color = levelColor(level * (i + 0.5f) / segments),
                        startAngle = 180f + i * segmentSweep,
                        // overlap neighbours slightly so no hairline shows between segments
                        sweepAngle = if (isLast) segmentSweep else segmentSweep + 0.4f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth,
                            cap = if (isLast) StrokeCap.Round else StrokeCap.Butt
                        )
                    )
                }
            }

            // tick marks
            var tickValue = MIN_VALUE
            while (tickValue <= MAX_VALUE + 0.01f) {
                val isMajor = (tickValue % MAJOR_STEP) < 0.01f
                val angleDeg = 180f + (tickValue / MAX_VALUE) * 180f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()

                val tickOuter = outerRadius - strokeWidth * 0.9f
                val tickInner = if (isMajor) tickOuter - w * 0.05f else tickOuter - w * 0.028f

                val p1 = Offset(center.x + tickOuter * cosA, center.y + tickOuter * sinA)
                val p2 = Offset(center.x + tickInner * cosA, center.y + tickInner * sinA)

                drawLine(
                    color = if (isMajor) minorTickColor else minorTickColor.copy(alpha = 0.6f),
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) w * 0.008f else w * 0.005f,
                    cap = StrokeCap.Round
                )
                tickValue += MINOR_STEP
            }

            // axis labels: 0, 60, 120
            val paint = Paint().apply {
                color = labelColor
                textSize = w * 0.045f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val labelRadius = outerRadius - w * 0.11f
            listOf(0f, 60f, 120f).forEach { labelValue ->
                val angleDeg = 180f + (labelValue / MAX_VALUE) * 180f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val lx = center.x + labelRadius * cos(angleRad).toFloat()
                val ly = center.y + labelRadius * sin(angleRad).toFloat() - (paint.ascent() + paint.descent()) / 2
                drawContext.canvas.nativeCanvas.drawText(labelValue.toInt().toString(), lx, ly, paint)
            }

            // needle, tinted to match the level it is pointing at
            val needleColor = levelColor(level)
            val needleAngleDeg = 180f + (animatedValue / MAX_VALUE) * 180f
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = outerRadius * 0.72f
            val needleEnd = Offset(
                center.x + needleLength * cos(needleAngleRad).toFloat(),
                center.y + needleLength * sin(needleAngleRad).toFloat()
            )
            drawLine(
                color = needleColor,
                start = center,
                end = needleEnd,
                strokeWidth = w * 0.02f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = needleColor,
                radius = w * 0.028f,
                center = center
            )
            drawCircle(
                color = hubColor,
                radius = w * 0.01f,
                center = center
            )
        }
    }
}
