package com.lmpnearme.europe.data.models

data class PricePoint(
    val hourLabel: String,
    val price: Double,
    val isNow: Boolean = false,
    val isFuture: Boolean = false
)

data class GenerationSource(
    val name: String,
    val percentShare: Double,
    val colorHex: String
)

data class ElectricitySnapshot(
    val zoneName: String,
    val zoneCode: String,
    val countryCode: String,
    val currentPriceEur: Double?,
    val priceChange: Double?,
    val dayAheadPrices: List<PricePoint>,
    val todayMinPrice: Double?,
    val todayMaxPrice: Double?,
    val currentLoad: Double?,
    val peakLoad: Double?,
    val generationMix: List<GenerationSource>,
    val carbonFreePercent: Double?,
    val lastUpdated: Long = System.currentTimeMillis(),
    val dataSource: String = "ENTSO-E"
)

enum class PriceLevel { GREAT, FAIR, HIGH, UNKNOWN }

fun ElectricitySnapshot.priceLevel(): PriceLevel {
    val min = todayMinPrice ?: return PriceLevel.UNKNOWN
    val max = todayMaxPrice ?: return PriceLevel.UNKNOWN
    val current = currentPriceEur ?: return PriceLevel.UNKNOWN
    if (max == min) return PriceLevel.FAIR
    val ratio = (current - min) / (max - min)
    return when {
        ratio < 0.30 -> PriceLevel.GREAT
        ratio < 0.65 -> PriceLevel.FAIR
        else -> PriceLevel.HIGH
    }
}

fun ElectricitySnapshot.priceLevelMessage(): String = when (priceLevel()) {
    PriceLevel.GREAT -> "Great time to use power"
    PriceLevel.FAIR -> "Fair time to use power"
    PriceLevel.HIGH -> "Expensive time to use power"
    PriceLevel.UNKNOWN -> "Typical conditions"
}
