package com.lmpnearme.europe.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lmpnearme.europe.data.models.ElectricitySnapshot
import com.lmpnearme.europe.data.models.PriceLevel
import com.lmpnearme.europe.data.models.priceLevelMessage
import com.lmpnearme.europe.data.models.priceLevel
import com.lmpnearme.europe.ui.components.LoadCard
import com.lmpnearme.europe.ui.components.PriceBarChart
import com.lmpnearme.europe.ui.components.PriceRangeCard
import com.lmpnearme.europe.ui.components.ResourceMixCard
import com.lmpnearme.europe.ui.theme.*
import com.lmpnearme.europe.utils.BiddingZone
import com.lmpnearme.europe.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    uiState: UiState,
    currentZone: BiddingZone?,
    availableZones: List<BiddingZone>,
    onRefresh: () -> Unit,
    onClearApiKey: () -> Unit,
    onSelectZone: (BiddingZone) -> Unit,
    modifier: Modifier = Modifier
) {
    var showZonePicker by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientTop, GradientBottom)))
    ) {
        when (uiState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(message = uiState.message, onRetry = onRefresh)
            is UiState.Success -> SuccessView(
                snapshot = uiState.snapshot,
                onRefresh = onRefresh,
                onZoneClick = { showZonePicker = true }
            )
            else -> {}
        }

        // Top action buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState is UiState.Success) {
                CircleIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    onClick = onRefresh
                )
            }
            Box {
                CircleIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = { showSettingsMenu = true }
                )
                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Change API Key") },
                        onClick = {
                            showSettingsMenu = false
                            onClearApiKey()
                        },
                        leadingIcon = { Icon(Icons.Default.Key, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Select Zone") },
                        onClick = {
                            showSettingsMenu = false
                            showZonePicker = true
                        },
                        leadingIcon = { Icon(Icons.Default.Place, null) }
                    )
                }
            }
        }

        if (showZonePicker) {
            ZonePickerDialog(
                zones = availableZones,
                currentZone = currentZone,
                onZoneSelected = { zone ->
                    showZonePicker = false
                    onSelectZone(zone)
                },
                onDismiss = { showZonePicker = false }
            )
        }
    }
}

@Composable
private fun SuccessView(
    snapshot: ElectricitySnapshot,
    onRefresh: () -> Unit,
    onZoneClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MM/dd/yyyy HH:mm z", Locale.getDefault()) }
    val updatedAt = timeFormat.format(Date(snapshot.lastUpdated))
    val priceLevel = snapshot.priceLevel()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // Zone header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = snapshot.zoneName,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onZoneClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = snapshot.zoneCode,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            // Current price hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val price = snapshot.currentPriceEur
                Text(
                    text = if (price != null) "€${price.roundToInt()}" else "—",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary
                )
                Text(
                    text = "/MWh · day-ahead LMP",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(12.dp))

                val levelColor = when (priceLevel) {
                    PriceLevel.GREAT -> Color(0xFF27AE60)
                    PriceLevel.FAIR -> Color(0xFF2ECC71)
                    PriceLevel.HIGH -> Color(0xFFE74C3C)
                    PriceLevel.UNKNOWN -> TextSecondary
                }
                Text(
                    text = snapshot.priceLevelMessage(),
                    color = levelColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                if (snapshot.priceChange != null) {
                    val changeAbs = abs(snapshot.priceChange)
                    val sign = if (snapshot.priceChange >= 0) "▲" else "▼"
                    val changeColor = if (snapshot.priceChange >= 0) Color(0xFFE74C3C) else Color(0xFF27AE60)
                    Text(
                        text = "$sign €${changeAbs.roundToInt()} in the last hour · Typical conditions",
                        color = changeColor,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // Day-ahead price chart card
        if (snapshot.dayAheadPrices.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        Text(
                            text = "NEXT HOURS · €/MWH · DAY-AHEAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(16.dp))
                        PriceBarChart(pricePoints = snapshot.dayAheadPrices)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Load + Range cards row
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    LoadCard(
                        loadGw = snapshot.currentLoad,
                        peakGw = snapshot.peakLoad
                    )
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    PriceRangeCard(
                        minPrice = snapshot.todayMinPrice,
                        maxPrice = snapshot.todayMaxPrice,
                        currentPrice = snapshot.currentPriceEur
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Resource mix card
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                ResourceMixCard(
                    sources = snapshot.generationMix,
                    carbonFreePercent = snapshot.carbonFreePercent,
                    lastUpdated = updatedAt
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Footer
        item {
            Text(
                text = "${snapshot.dataSource} · $updatedAt",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = TextPrimary)
            Spacer(Modifier.height(16.dp))
            Text("Loading electricity data...", color = TextSecondary)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFF39C12), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Could not load data",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlueLight)
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(0.5.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBackground)
            .border(0.5.dp, CardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = TextPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ZonePickerDialog(
    zones: List<BiddingZone>,
    currentZone: BiddingZone?,
    onZoneSelected: (BiddingZone) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bidding Zone") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(zones) { zone ->
                    val isSelected = zone.eicCode == currentZone?.eicCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onZoneSelected(zone) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Spacer(Modifier.width(28.dp))
                        }
                        Column {
                            Text(
                                text = zone.displayName,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = zone.eicCode,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
