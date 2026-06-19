package com.lmpnearme.europe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lmpnearme.europe.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun LoadCard(
    loadGw: Double?,
    peakGw: Double?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "LOAD",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = TextMuted
        )
        Spacer(Modifier.height(8.dp))
        if (loadGw != null) {
            Text(
                text = "${"%.1f".format(loadGw)} GW",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            val fraction = if (peakGw != null && peakGw > 0) (loadGw / peakGw).coerceIn(0.0, 1.0).toFloat() else 0.5f
            LoadBar(fraction = fraction)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (peakGw != null) "${"%.0f".format(fraction * 100)}% of today's peak" else "Current load",
                fontSize = 12.sp,
                color = TextMuted
            )
        } else {
            Text("—", fontSize = 28.sp, fontWeight = FontWeight.Light, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Data unavailable", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun LoadBar(fraction: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val width = size.width
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(0f, size.height / 2),
            end = Offset(width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(0f, size.height / 2),
            end = Offset(width * fraction, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PriceRangeCard(
    minPrice: Double?,
    maxPrice: Double?,
    currentPrice: Double?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "TODAY'S RANGE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = TextMuted
        )
        Text(
            text = "DAY-AHEAD",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = TextMuted
        )
        Spacer(Modifier.height(8.dp))
        if (minPrice != null && maxPrice != null) {
            Text(
                text = "€${minPrice.roundToInt()} – €${maxPrice.roundToInt()}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            GradientBar()
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("low", fontSize = 11.sp, color = TextMuted)
                Text("high", fontSize = 11.sp, color = TextMuted)
            }
            if (currentPrice != null && maxPrice > minPrice) {
                Spacer(Modifier.height(6.dp))
                val ratio = (currentPrice - minPrice) / (maxPrice - minPrice)
                val posText = when {
                    ratio < 0.33 -> "You're near the low end"
                    ratio > 0.67 -> "You're near the high end"
                    else -> "You're in the middle range"
                }
                Text(posText, fontSize = 12.sp, color = TextMuted)
            }
        } else {
            Text("—", fontSize = 22.sp, fontWeight = FontWeight.Light, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Data unavailable", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun GradientBar() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF27AE60),
                    Color(0xFFF1C40F),
                    Color(0xFFE74C3C)
                )
            ),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
    }
}
