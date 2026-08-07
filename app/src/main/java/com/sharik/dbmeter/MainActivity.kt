package com.sharik.dbmeter

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.sharik.dbmeter.ui.DbChart
import com.sharik.dbmeter.ui.DbGauge
import com.sharik.dbmeter.ui.NativeAdCard
import com.sharik.dbmeter.ui.ReferenceChartDialog
import com.sharik.dbmeter.ui.ReferenceChip
import com.sharik.dbmeter.ui.theme.ButtonPrimary
import com.sharik.dbmeter.ui.theme.ButtonSecondaryBorder
import com.sharik.dbmeter.ui.theme.CardBackground
import com.sharik.dbmeter.ui.theme.DbMeterTheme
import com.sharik.dbmeter.ui.theme.ScreenBackground
import com.sharik.dbmeter.ui.theme.TextPrimary
import com.sharik.dbmeter.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WINDOW_SECONDS = 15f
private const val POLL_INTERVAL_MS = 200L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ads SDK startup touches disk and network, so keep it off the main thread.
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity)
        }

        setContent {
            DbMeterTheme {
                Surface(color = ScreenBackground) {
                    DbMeterApp()
                }
            }
        }
    }
}

@Composable
fun DbMeterApp() {
    val context = LocalContext.current
    val soundMeter = remember { SoundMeter() }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var startAfterGrant by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var currentDb by remember { mutableFloatStateOf(0f) }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    val readings = remember { mutableStateListOf<Pair<Float, Float>>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted && startAfterGrant) {
            isRunning = true
        }
        startAfterGrant = false
    }

    val onStart: () -> Unit = {
        if (hasPermission) {
            isRunning = true
        } else {
            startAfterGrant = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val onStop: () -> Unit = { isRunning = false }
    val onReset: () -> Unit = {
        isRunning = false
        readings.clear()
        currentDb = 0f
        elapsedSeconds = 0f
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            soundMeter.start()
            val baseElapsed = elapsedSeconds
            val startClock = SystemClock.elapsedRealtime()
            try {
                while (isRunning) {
                    val db = soundMeter.readDecibel().toFloat()
                    currentDb = db
                    elapsedSeconds = baseElapsed + (SystemClock.elapsedRealtime() - startClock) / 1000f
                    readings.add(elapsedSeconds to db)
                    delay(POLL_INTERVAL_MS)
                }
            } finally {
                soundMeter.stop()
            }
        }
    }

    val minDb = readings.minOfOrNull { it.second } ?: 0f
    val maxDb = readings.maxOfOrNull { it.second } ?: 0f
    val avgDb = if (readings.isNotEmpty()) readings.map { it.second }.average().toFloat() else 0f
    val windowPoints = readings.filter { it.first >= elapsedSeconds - WINDOW_SECONDS }

    DbMeterScreen(
        currentDb = currentDb,
        minDb = minDb,
        avgDb = avgDb,
        maxDb = maxDb,
        chartPoints = windowPoints,
        onStart = onStart,
        onStop = onStop,
        onReset = onReset
    )
}

@Composable
fun DbMeterScreen(
    currentDb: Float,
    minDb: Float,
    avgDb: Float,
    maxDb: Float,
    chartPoints: List<Pair<Float, Float>>,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    var showReference by remember { mutableStateOf(false) }
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (showReference) {
        ReferenceChartDialog(
            currentDb = currentDb,
            onDismiss = { showReference = false }
        )
    }

    Scaffold(
        containerColor = ScreenBackground,
        // The ad lives in the bottom bar so it is measured before the content
        // column. Inside the column it was the last child, so on a full screen
        // it got whatever height was left over -- which was not enough.
        bottomBar = {
            NativeAdCard(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = if (isLandscape) 10.dp else 20.dp)

        if (isLandscape) {
            LandscapeContent(
                currentDb = currentDb,
                minDb = minDb,
                avgDb = avgDb,
                maxDb = maxDb,
                chartPoints = chartPoints,
                onStart = onStart,
                onStop = onStop,
                onReset = onReset,
                onReferenceClick = { showReference = true },
                modifier = contentModifier
            )
        } else {
            PortraitContent(
                currentDb = currentDb,
                minDb = minDb,
                avgDb = avgDb,
                maxDb = maxDb,
                chartPoints = chartPoints,
                onStart = onStart,
                onStop = onStop,
                onReset = onReset,
                onReferenceClick = { showReference = true },
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun PortraitContent(
    currentDb: Float,
    minDb: Float,
    avgDb: Float,
    maxDb: Float,
    chartPoints: List<Pair<Float, Float>>,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onReferenceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        GaugeCard(
            currentDb = currentDb,
            gaugeWidthFraction = 0.82f,
            compact = false,
            onReferenceClick = onReferenceClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        StatsRow(minDb = minDb, avgDb = avgDb, maxDb = maxDb, compact = false)

        Spacer(modifier = Modifier.height(14.dp))

        // The chart takes the leftover height, so a short screen shrinks it
        // instead of squashing the controls underneath.
        ChartCard(
            points = chartPoints,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Controls(onStart = onStart, onStop = onStop, onReset = onReset, compact = false)
    }
}

/**
 * Landscape puts the dial on the left and gives the right column to the chart,
 * which is what benefits from the extra width. Everything is tightened up: a
 * phone on its side has roughly 300dp of usable height, and the ad takes a
 * chunk of that.
 */
@Composable
private fun LandscapeContent(
    currentDb: Float,
    minDb: Float,
    avgDb: Float,
    maxDb: Float,
    chartPoints: List<Pair<Float, Float>>,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onReferenceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scrolls rather than clips if the dial still will not fit on a very
        // short screen.
        Column(
            modifier = Modifier
                .weight(0.9f)
                .verticalScroll(rememberScrollState())
        ) {
            GaugeCard(
                currentDb = currentDb,
                gaugeWidthFraction = 0.58f,
                compact = true,
                onReferenceClick = onReferenceClick
            )
        }

        Column(modifier = Modifier.weight(1.1f)) {
            StatsRow(minDb = minDb, avgDb = avgDb, maxDb = maxDb, compact = true)

            Spacer(modifier = Modifier.height(8.dp))

            ChartCard(
                points = chartPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Controls(onStart = onStart, onStop = onStop, onReset = onReset, compact = true)
        }
    }
}

@Composable
private fun StatsRow(
    minDb: Float,
    avgDb: Float,
    maxDb: Float,
    compact: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        StatCard("%.0f dB".format(minDb), "MIN", compact, Modifier.weight(1f))
        StatCard("%.0f dB".format(avgDb), "AVG", compact, Modifier.weight(1f))
        StatCard("%.0f dB".format(maxDb), "MAX", compact, Modifier.weight(1f))
    }
}

@Composable
private fun ChartCard(
    points: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        DbChart(
            points = points,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Composable
private fun Controls(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    compact: Boolean
) {
    val buttonHeight = if (compact) 44.dp else 52.dp
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
        ) {
            Text("Start", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onStop,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, ButtonSecondaryBorder),
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
        ) {
            Text("Stop", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, ButtonSecondaryBorder),
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
        ) {
            Text("Reset", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GaugeCard(
    currentDb: Float,
    gaugeWidthFraction: Float,
    compact: Boolean,
    onReferenceClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (compact) 10.dp else 14.dp,
                    bottom = if (compact) 12.dp else 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DbGauge(value = currentDb, modifier = Modifier.fillMaxWidth(gaugeWidthFraction))
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.0f".format(currentDb),
                    fontSize = if (compact) 28.sp else 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "dB",
                    fontSize = if (compact) 15.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = if (compact) 3.dp else 5.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            ReferenceChip(currentDb = currentDb, onClick = onReferenceClick)
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (compact) 8.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = if (compact) 15.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
