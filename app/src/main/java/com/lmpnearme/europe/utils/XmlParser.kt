package com.lmpnearme.europe.utils

import android.util.Xml
import com.lmpnearme.europe.data.models.GenerationSource
import com.lmpnearme.europe.data.models.PricePoint
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object XmlParser {

    // ENTSO-E XML responses use "yyyy-MM-dd'T'HH:mmZ" (no seconds) per official schema.
    // Java's Instant.parse() requires seconds, so we need a custom formatter.
    private val entsoFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
    private val compactFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        .withZone(ZoneId.of("UTC"))

    private fun parseEntsoDate(text: String): Instant? {
        val trimmed = text.trim()
        // Standard ISO-8601 with seconds (e.g. "2026-06-19T00:00:00Z")
        try { return Instant.parse(trimmed) } catch (_: Exception) {}
        // ENTSO-E format without seconds (e.g. "2026-06-19T00:00Z") — official schema pages 7, 11, 14
        try { return entsoFormat.parse(trimmed, Instant::from) } catch (_: Exception) {}
        // Compact request format (e.g. "202606190000")
        try { return Instant.from(compactFormat.parse(trimmed)) } catch (_: Exception) {}
        return null
    }

    fun parseDayAheadPrices(xml: String): List<Pair<Long, Double>> {
        val results = mutableListOf<Pair<Long, Double>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var inTimeSeries = false
            var inPeriod = false
            var inPoint = false
            var periodStart: Instant? = null
            var resolution = 60
            var position = 0
            var price = 0.0

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (event) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "TimeSeries" -> inTimeSeries = true
                        "Period" -> if (inTimeSeries) inPeriod = true
                        "timeInterval" -> {}
                        "start" -> if (inPeriod) {
                            val text = parser.nextText().trim()
                            periodStart = parseEntsoDate(text)
                        }
                        "resolution" -> if (inPeriod) {
                            val res = parser.nextText().trim()
                            resolution = parseResolutionMinutes(res)
                        }
                        "Point" -> if (inPeriod) { inPoint = true; position = 0; price = 0.0 }
                        "position" -> if (inPoint) position = parser.nextText().trim().toIntOrNull() ?: 0
                        "price.amount" -> if (inPoint) price = parser.nextText().trim().toDoubleOrNull() ?: 0.0
                    }
                    XmlPullParser.END_TAG -> when (tag) {
                        "TimeSeries" -> inTimeSeries = false
                        "Period" -> inPeriod = false
                        "Point" -> {
                            if (inPeriod && periodStart != null && position > 0) {
                                val epochMs = periodStart!!.toEpochMilli() + (position - 1).toLong() * resolution * 60 * 1000
                                results.add(Pair(epochMs, price))
                            }
                            inPoint = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}
        return results.sortedBy { it.first }
    }

    fun parseActualLoad(xml: String): Pair<Double?, Double?> {
        var latestLoad: Double? = null
        var maxLoad: Double? = null
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var inPoint = false
            var quantity = 0.0
            val allQuantities = mutableListOf<Double>()

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (event) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "Point" -> { inPoint = true; quantity = 0.0 }
                        "quantity" -> if (inPoint) quantity = parser.nextText().trim().toDoubleOrNull() ?: 0.0
                    }
                    XmlPullParser.END_TAG -> when (tag) {
                        "Point" -> {
                            if (quantity > 0) allQuantities.add(quantity)
                            inPoint = false
                        }
                    }
                }
                event = parser.next()
            }
            if (allQuantities.isNotEmpty()) {
                latestLoad = allQuantities.last()
                maxLoad = allQuantities.max()
            }
        } catch (_: Exception) {}
        return Pair(latestLoad, maxLoad)
    }

    fun parseGenerationMix(xml: String): List<GenerationSource> {
        val rawData = mutableMapOf<String, Double>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var inTimeSeries = false
            var inPoint = false
            var psrType = ""
            var latestQuantity = 0.0
            val latestQuantities = mutableMapOf<String, MutableList<Double>>()

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (event) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "TimeSeries" -> { inTimeSeries = true; psrType = ""; latestQuantity = 0.0 }
                        "psrType" -> if (inTimeSeries) psrType = parser.nextText().trim()
                        "Point" -> if (inTimeSeries) { inPoint = true; latestQuantity = 0.0 }
                        "quantity" -> if (inPoint) latestQuantity = parser.nextText().trim().toDoubleOrNull() ?: 0.0
                    }
                    XmlPullParser.END_TAG -> when (tag) {
                        "TimeSeries" -> inTimeSeries = false
                        "Point" -> {
                            if (psrType.isNotEmpty() && latestQuantity > 0) {
                                latestQuantities.getOrPut(psrType) { mutableListOf() }.add(latestQuantity)
                            }
                            inPoint = false
                        }
                    }
                }
                event = parser.next()
            }
            latestQuantities.forEach { (type, list) ->
                if (list.isNotEmpty()) rawData[type] = list.last()
            }
        } catch (_: Exception) {}

        return mapToGenerationSources(rawData)
    }

    private fun mapToGenerationSources(raw: Map<String, Double>): List<GenerationSource> {
        if (raw.isEmpty()) return emptyList()
        val grouped = mutableMapOf(
            "Nuclear" to 0.0,
            "Gas" to 0.0,
            "Coal" to 0.0,
            "Hydro" to 0.0,
            "Wind" to 0.0,
            "Solar" to 0.0,
            "Oil" to 0.0,
            "Biomass" to 0.0,
            "Other Renewables" to 0.0,
            "Other" to 0.0
        )
        raw.forEach { (psrType, mw) ->
            when (psrType) {
                "B14" -> grouped["Nuclear"] = grouped["Nuclear"]!! + mw
                "B04" -> grouped["Gas"] = grouped["Gas"]!! + mw
                "B02", "B05" -> grouped["Coal"] = grouped["Coal"]!! + mw
                "B10", "B11", "B12" -> grouped["Hydro"] = grouped["Hydro"]!! + mw
                "B18", "B19" -> grouped["Wind"] = grouped["Wind"]!! + mw
                "B16" -> grouped["Solar"] = grouped["Solar"]!! + mw
                "B06" -> grouped["Oil"] = grouped["Oil"]!! + mw
                "B01", "B17" -> grouped["Biomass"] = grouped["Biomass"]!! + mw
                "B09", "B13", "B15" -> grouped["Other Renewables"] = grouped["Other Renewables"]!! + mw
                else -> grouped["Other"] = grouped["Other"]!! + mw
            }
        }
        val total = grouped.values.sum().takeIf { it > 0 } ?: return emptyList()
        val colorMap = mapOf(
            "Nuclear" to "#9C27B0",
            "Gas" to "#FF5722",
            "Coal" to "#795548",
            "Hydro" to "#2196F3",
            "Wind" to "#4CAF50",
            "Solar" to "#FFC107",
            "Oil" to "#607D8B",
            "Biomass" to "#8BC34A",
            "Other Renewables" to "#00BCD4",
            "Other" to "#9E9E9E"
        )
        return grouped.entries
            .filter { it.value > 0 }
            .map { (name, mw) ->
                GenerationSource(name, (mw / total) * 100.0, colorMap[name] ?: "#9E9E9E")
            }
            .sortedByDescending { it.percentShare }
    }

    private fun parseResolutionMinutes(iso8601Duration: String): Int = when (iso8601Duration) {
        "PT15M" -> 15
        "PT30M" -> 30
        "PT60M", "PT1H" -> 60
        else -> 60
    }
}
