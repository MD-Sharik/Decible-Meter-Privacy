package com.sharik.dbmeter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sharik.dbmeter.ui.theme.DbTheme

/** A familiar sound at a given level. */
data class SoundReference(val db: Int, val label: String)

/** Loudest first, so [referenceFor] can take the first match. */
val SoundReferences = listOf(
    SoundReference(140, "Gun Shots"),
    SoundReference(130, "Ambulance"),
    SoundReference(120, "Thunder"),
    SoundReference(110, "Concerts"),
    SoundReference(100, "Subway Train"),
    SoundReference(90, "Motorcycle"),
    SoundReference(80, "Busy Traffic"),
    SoundReference(70, "Restaurant"),
    SoundReference(60, "Conversation"),
    SoundReference(50, "Quiet Office"),
    SoundReference(40, "Quiet Library"),
    SoundReference(30, "Whisper"),
    SoundReference(20, "Clock Ticking"),
    SoundReference(10, "Breathing")
)

/** Loudest reference at or below [db], or null when it is quieter than all of them. */
fun referenceFor(db: Float): SoundReference? = SoundReferences.firstOrNull { db >= it.db }

/**
 * Compact readout of which reference band the current level falls into.
 * Tapping it opens the full [ReferenceChartDialog].
 */
@Composable
fun ReferenceChip(
    currentDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reference = referenceFor(currentDb)
    val label = reference?.let { "${it.db}dB : ${it.label}" } ?: "Reference Chart"

    Surface(
        shape = RoundedCornerShape(50),
        color = DbTheme.colors.chipBackground,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(levelColor(currentDb / 120f))
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DbTheme.colors.textPrimary
            )
        }
    }
}

@Composable
fun ReferenceChartDialog(
    currentDb: Float,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DbTheme.colors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "%.1f dB".format(currentDb),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = DbTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Reference Chart",
                    fontSize = 15.sp,
                    color = DbTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Capped and scrollable so the list still works in landscape,
                // where the dialog has very little height to play with.
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SoundReferences.forEach { reference ->
                        ReferenceRow(reference)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceRow(reference: SoundReference) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 26.dp, height = 18.dp)
                .clip(RoundedCornerShape(4.dp))
                // Same ramp as the gauge stripe, so a swatch here and a colour
                // on the dial mean the same thing.
                .background(levelColor(reference.db / 120f))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${reference.db}dB : ${reference.label}",
            fontSize = 14.sp,
            color = DbTheme.colors.textPrimary
        )
    }
}
