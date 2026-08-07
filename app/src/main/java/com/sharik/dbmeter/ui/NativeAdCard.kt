package com.sharik.dbmeter.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.sharik.dbmeter.R
import com.sharik.dbmeter.ui.theme.DbTheme

private const val TAG = "NativeAdCard"

/**
 * AdMob native ad rendered as a card matching the rest of the screen.
 *
 * Native ads have to be laid out with real Views so the SDK can register each
 * asset for click and impression tracking, hence the inflated [NativeAdView]
 * inside an [AndroidView] rather than pure Compose. Nothing is drawn until an
 * ad arrives, so a no-fill leaves no empty gap.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    // NativeAdView cannot be inflated by the layout preview renderer.
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val unitId = stringResource(R.string.admob_native_unit_id)
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(unitId) {
        var disposed = false
        AdLoader.Builder(context, unitId)
            .forNativeAd { ad ->
                // The callback can land after we have left composition.
                if (disposed) {
                    ad.destroy()
                } else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.code} ${error.message}")
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())

        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DbTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView
            },
            update = { adView -> adView.bind(ad) }
        )
    }
}

private fun NativeAdView.bind(ad: NativeAd) {
    val headline = findViewById<TextView>(R.id.ad_headline)
    val body = findViewById<TextView>(R.id.ad_body)
    val callToAction = findViewById<TextView>(R.id.ad_call_to_action)
    val appIcon = findViewById<ImageView>(R.id.ad_app_icon)

    // Every asset view has to be registered before setNativeAd, or the SDK
    // will not track clicks on it.
    headlineView = headline
    bodyView = body
    callToActionView = callToAction
    iconView = appIcon

    headline.text = ad.headline

    // Optional assets: hide the view rather than showing an empty one.
    body.text = ad.body.orEmpty()
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE

    callToAction.text = ad.callToAction.orEmpty()
    callToAction.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE

    val iconDrawable = ad.icon?.drawable
    appIcon.setImageDrawable(iconDrawable)
    appIcon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE

    setNativeAd(ad)
}
