package com.sharik.dbmeter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sharik.dbmeter.SoundMeter
import com.sharik.dbmeter.ui.theme.ButtonPrimary
import com.sharik.dbmeter.ui.theme.CardBackground
import com.sharik.dbmeter.ui.theme.ScreenBackground
import com.sharik.dbmeter.ui.theme.TextPrimary
import com.sharik.dbmeter.ui.theme.TextSecondary

/** Opens the [CalibrationDialog]. Sits beside the reference chip. */
@Composable
fun CalibrateChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = ScreenBackground,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = "Calibrate",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Lets the user trim the reading to match a reference they trust.
 *
 * Phone mics differ by more than 10 dB between models, so a single built-in
 * offset cannot be right everywhere. The reading stays live while the slider
 * moves, which is the whole point: you hold a known meter next to the phone
 * and nudge until the two agree.
 */
@Composable
fun CalibrationDialog(
    currentDb: Float,
    adjustment: Float,
    onAdjustmentChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "%.1f dB".format(currentDb),
                    fontSize = 30.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Calibration",
                    fontSize = 15.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "%+.1f dB".format(adjustment),
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = TextPrimary
                )

                Slider(
                    value = adjustment,
                    onValueChange = onAdjustmentChange,
                    valueRange = -SoundMeter.MAX_ADJUSTMENT..SoundMeter.MAX_ADJUSTMENT,
                    // 0.5 dB detents across the whole range.
                    steps = 59,
                    colors = SliderDefaults.colors(
                        thumbColor = ButtonPrimary,
                        activeTrackColor = ButtonPrimary,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Hold a meter you trust next to the phone and slide " +
                        "until the two readings agree. Saved for next time.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { onAdjustmentChange(0f) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset", color = TextSecondary)
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
