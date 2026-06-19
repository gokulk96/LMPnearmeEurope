package com.lmpnearme.europe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lmpnearme.europe.data.models.GenerationSource
import com.lmpnearme.europe.ui.theme.TextMuted
import com.lmpnearme.europe.ui.theme.TextPrimary
import com.lmpnearme.europe.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ResourceMixCard(
    sources: List<GenerationSource>,
    carbonFreePercent: Double?,
    lastUpdated: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RESOURCE MIX",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = TextMuted
            )
            if (carbonFreePercent != null) {
                Text(
                    text = "${carbonFreePercent.roundToInt()}% carbon-free",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        if (sources.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            StackedBar(sources = sources)
            Spacer(Modifier.height(16.dp))
            GenerationLegend(sources = sources)
        } else {
            Spacer(Modifier.height(12.dp))
            Text("Generation data unavailable", color = TextMuted, fontSize = 13.sp)
        }

        if (lastUpdated.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(text = lastUpdated, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun StackedBar(sources: List<GenerationSource>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
    ) {
        sources.forEach { source ->
            val weight = source.percentShare.toFloat().coerceAtLeast(0.5f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .background(parseHexColor(source.colorHex))
            )
        }
    }
}

@Composable
private fun GenerationLegend(sources: List<GenerationSource>) {
    val chunked = sources.chunked(2)
    chunked.forEach { pair ->
        Row(modifier = Modifier.fillMaxWidth()) {
            pair.forEach { source ->
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(source.colorHex))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${source.name} ${source.percentShare.roundToInt()}%",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color.Gray
    }
}
