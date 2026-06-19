package com.lmpnearme.europe.data.repository

import com.lmpnearme.europe.data.api.EntsoApiClient
import com.lmpnearme.europe.data.models.ElectricitySnapshot
import com.lmpnearme.europe.data.models.PricePoint
import com.lmpnearme.europe.utils.BiddingZone
import com.lmpnearme.europe.utils.XmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ElectricityRepository(private val apiClient: EntsoApiClient = EntsoApiClient()) {

    private val hourFormat = DateTimeFormatter.ofPattern("ha").withZone(ZoneId.systemDefault())
    private val hourFormatShort = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    suspend fun fetchSnapshot(apiKey: String, zone: BiddingZone): Result<ElectricitySnapshot> =
        withContext(Dispatchers.IO) {
            try {
                val pricesDeferred = async { apiClient.fetchDayAheadPrices(apiKey, zone.eicCode) }
                val loadDeferred = async { apiClient.fetchActualLoad(apiKey, zone.eicCode) }
                val genDeferred = async { apiClient.fetchGenerationMix(apiKey, zone.eicCode) }

                val pricesXml = pricesDeferred.await()
                val loadXml = loadDeferred.await()
                val genXml = genDeferred.await()

                val allPrices = if (!pricesXml.isNullOrBlank()) {
                    XmlParser.parseDayAheadPrices(pricesXml)
                } else emptyList()

                val nowMs = System.currentTimeMillis()
                val currentHourMs = (nowMs / 3_600_000L) * 3_600_000L

                val currentPrice = allPrices.lastOrNull { it.first <= nowMs }?.second
                val previousHourPrice = allPrices.lastOrNull { it.first <= currentHourMs - 3_600_000L }?.second
                val priceChange = if (currentPrice != null && previousHourPrice != null) {
                    currentPrice - previousHourPrice
                } else null

                val todayStart = Instant.now().atZone(ZoneId.systemDefault())
                    .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val todayEnd = todayStart + 86_400_000L
                val todayPrices = allPrices.filter { it.first in todayStart until todayEnd }.map { it.second }
                val todayMin = todayPrices.minOrNull()
                val todayMax = todayPrices.maxOrNull()

                val nextHourPoints = buildNextHourPoints(allPrices, nowMs, currentPrice)

                val (currentLoad, peakLoad) = if (!loadXml.isNullOrBlank()) {
                    XmlParser.parseActualLoad(loadXml)
                } else Pair(null, null)

                val generationMix = if (!genXml.isNullOrBlank()) {
                    XmlParser.parseGenerationMix(genXml)
                } else emptyList()

                val carbonFreeTypes = setOf("Nuclear", "Wind", "Solar", "Hydro", "Other Renewables", "Biomass")
                val carbonFreePercent = if (generationMix.isNotEmpty()) {
                    generationMix.filter { it.name in carbonFreeTypes }.sumOf { it.percentShare }
                } else null

                Result.success(
                    ElectricitySnapshot(
                        zoneName = zone.displayName,
                        zoneCode = zone.eicCode,
                        countryCode = zone.countryCodes.first(),
                        currentPriceEur = currentPrice,
                        priceChange = priceChange,
                        dayAheadPrices = nextHourPoints,
                        todayMinPrice = todayMin,
                        todayMaxPrice = todayMax,
                        currentLoad = currentLoad?.div(1000.0),
                        peakLoad = peakLoad?.div(1000.0),
                        generationMix = generationMix,
                        carbonFreePercent = carbonFreePercent
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildNextHourPoints(
        allPrices: List<Pair<Long, Double>>,
        nowMs: Long,
        currentPrice: Double?
    ): List<PricePoint> {
        val points = mutableListOf<PricePoint>()
        val currentHourMs = (nowMs / 3_600_000L) * 3_600_000L
        val windowStart = currentHourMs
        val windowEnd = currentHourMs + 9 * 3_600_000L

        val relevant = allPrices.filter { it.first in windowStart until windowEnd }

        relevant.forEachIndexed { index, (epochMs, price) ->
            val instant = Instant.ofEpochMilli(epochMs)
            val label = if (index == 0) "Now" else hourFormat.format(instant).uppercase()
            points.add(
                PricePoint(
                    hourLabel = label,
                    price = price,
                    isNow = index == 0,
                    isFuture = epochMs > nowMs
                )
            )
        }

        if (points.isEmpty() && currentPrice != null) {
            points.add(PricePoint("Now", currentPrice, isNow = true))
        }

        return points
    }
}
