package com.lmpnearme.europe.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class EntsoApiClient {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    private val baseUrl = "https://web-api.tp.entsoe.eu/api"

    fun fetchDayAheadPrices(apiKey: String, zoneEic: String): String? {
        val now = Instant.now()
        val startOfYesterday = now.atZone(ZoneOffset.UTC)
            .toLocalDate().minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfTomorrow = now.atZone(ZoneOffset.UTC)
            .toLocalDate().plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant()

        val url = buildUrl(
            apiKey = apiKey,
            params = mapOf(
                "documentType" to "A44",
                "in_Domain" to zoneEic,
                "out_Domain" to zoneEic,
                "periodStart" to dateFormat.format(startOfYesterday),
                "periodEnd" to dateFormat.format(endOfTomorrow)
            )
        )
        return executeGet(url)
    }

    fun fetchActualLoad(apiKey: String, zoneEic: String): String? {
        val now = Instant.now()
        val startOfDay = now.atZone(ZoneOffset.UTC)
            .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = now.atZone(ZoneOffset.UTC)
            .toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        val url = buildUrl(
            apiKey = apiKey,
            params = mapOf(
                "documentType" to "A65",
                "processType" to "A16",
                "outBiddingZone_Domain" to zoneEic,
                "periodStart" to dateFormat.format(startOfDay),
                "periodEnd" to dateFormat.format(endOfDay)
            )
        )
        return executeGet(url)
    }

    fun fetchGenerationMix(apiKey: String, zoneEic: String): String? {
        val now = Instant.now()
        val startOfDay = now.atZone(ZoneOffset.UTC)
            .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        val endOfDay = now.atZone(ZoneOffset.UTC)
            .toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        val url = buildUrl(
            apiKey = apiKey,
            params = mapOf(
                "documentType" to "A75",
                "processType" to "A16",
                "in_Domain" to zoneEic,
                "periodStart" to dateFormat.format(startOfDay),
                "periodEnd" to dateFormat.format(endOfDay)
            )
        )
        return executeGet(url)
    }

    private fun buildUrl(apiKey: String, params: Map<String, String>): String {
        val query = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$baseUrl?securityToken=$apiKey&$query"
    }

    private fun executeGet(url: String): String? = try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    } catch (_: Exception) { null }
}
