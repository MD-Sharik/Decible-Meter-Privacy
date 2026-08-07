package com.sharik.dbmeter

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    Scaffold(containerColor = ScreenBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            GaugeCard(currentDb = currentDb)

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("%.0f dB".format(minDb), "MIN", Modifier.weight(1f))
                StatCard("%.0f dB".format(avgDb), "AVG", Modifier.weight(1f))
                StatCard("%.0f dB".format(maxDb), "MAX", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DbChart(
                    points = chartPoints,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
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
                        .height(52.dp)
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
                        .height(52.dp)
                ) {
                    Text("Reset", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NativeAdCard()
        }
    }
}

@Composable
private fun GaugeCard(currentDb: Float) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DbGauge(value = currentDb, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.0f".format(currentDb),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "dB",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
