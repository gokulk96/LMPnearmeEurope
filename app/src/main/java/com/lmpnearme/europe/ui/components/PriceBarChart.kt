package com.lmpnearme.europe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lmpnearme.europe.data.models.PricePoint
import com.lmpnearme.europe.ui.theme.BarColorBlue
import com.lmpnearme.europe.ui.theme.BarColorYellow
import com.lmpnearme.europe.ui.theme.TextMuted
import com.lmpnearme.europe.ui.theme.TextPrimary
import kotlin.math.roundToInt

@Composable
fun PriceBarChart(
    pricePoints: List<PricePoint>,
    modifier: Modifier = Modifier
) {
    if (pricePoints.isEmpty()) return

    val maxPrice = pricePoints.maxOf { it.price }.coerceAtLeast(1.0)
    val highThreshold = maxPrice * 0.75

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        pricePoints.forEach { point ->
            PriceBar(point = point, maxPrice = maxPrice, highThreshold = highThreshold)
        }
    }
}

@Composable
private fun PriceBar(point: PricePoint, maxPrice: Double, highThreshold: Double) {
    val barColor = if (point.price >= highThreshold) BarColorYellow else BarColorBlue
    val heightFraction = (point.price / maxPrice).coerceIn(0.1, 1.0).toFloat()
    val maxBarHeight = 72.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(36.dp)
    ) {
        Text(
            text = point.hourLabel,
            fontSize = 9.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            fontWeight = if (point.isNow) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(maxBarHeight * heightFraction)
                .clip(RoundedCornerShape(5.dp))
                .background(barColor.copy(alpha = if (point.isFuture || point.isNow) 1f else 0.7f))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = point.price.roundToInt().toString(),
            fontSize = 10.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = if (point.isNow) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
