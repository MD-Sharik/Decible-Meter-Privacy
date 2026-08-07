package com.sharik.dbmeter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sharik.dbmeter.R

/**
 * Anchored AdMob banner. The unit id comes from resources so switching from the
 * test id to a real one is a strings.xml edit.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    // AdView cannot be instantiated by the layout preview renderer.
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val unitId = stringResource(R.string.admob_banner_unit_id)

    val adView = remember(unitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = unitId
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(factory = { adView })
    }
}
