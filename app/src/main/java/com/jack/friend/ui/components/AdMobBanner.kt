package com.jack.friend.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-7931782163570852/1286931706"
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.LARGE_BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
