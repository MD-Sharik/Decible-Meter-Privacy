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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.sharik.dbmeter.ui.theme.GaugeTrack
import com.sharik.dbmeter.ui.theme.GaugeTrackMinor
import com.sharik.dbmeter.ui.theme.NeedleGreen
import com.sharik.dbmeter.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

private const val MIN_VALUE = 0f
private const val MAX_VALUE = 120f
private const val MAJOR_STEP = 20f
private const val MINOR_STEP = 4f

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

    val trackColor = GaugeTrack
    val minorTickColor = GaugeTrackMinor
    val needleColor = NeedleGreen
    val labelColor = TextSecondary.toArgb()

    Box(modifier = modifier.fillMaxWidth()) {
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

            // background track arc (top semicircle)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

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

            // needle
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
                color = Color.White,
                radius = w * 0.01f,
                center = center
            )
        }
    }
}
