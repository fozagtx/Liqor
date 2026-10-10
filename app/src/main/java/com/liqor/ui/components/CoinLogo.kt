package com.liqor.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liqor.R
import com.liqor.ui.theme.Ground

@Composable
fun CoinLogo(
    asset: String,
    size: Dp,
    modifier: Modifier = Modifier,
    imageInset: Dp = 0.dp,
    containerColor: Color = Ground,
    borderColor: Color? = null,
) {
    val image = when (asset.uppercase()) {
        "SOL" -> R.drawable.logo_sol
        "ETH" -> R.drawable.logo_eth
        "BTC" -> R.drawable.logo_btc
        "JUP" -> R.drawable.logo_jup
        "HYPE" -> R.drawable.logo_hype
        "ZEC" -> R.drawable.logo_zec
        "USDC" -> R.drawable.logo_usdc
        "SPCX" -> R.drawable.logo_spcx
        "SNDK" -> R.drawable.logo_sndk
        "SKHYNIX" -> R.drawable.logo_skhynix
        else -> null
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .then(borderColor?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (image == null) {
            Text(
                asset.take(1),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        } else {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(imageInset).clip(CircleShape),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
