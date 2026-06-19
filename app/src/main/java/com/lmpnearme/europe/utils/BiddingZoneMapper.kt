package com.lmpnearme.europe.utils

data class BiddingZone(
    val eicCode: String,
    val displayName: String,
    val countryCodes: List<String>,
    val currency: String = "EUR"
)

object BiddingZoneMapper {

    private val zones = listOf(
        BiddingZone("10Y1001A1001A82H", "Germany / Luxembourg", listOf("DE", "LU")),
        BiddingZone("10YFR-RTE------C", "France", listOf("FR")),
        BiddingZone("10YES-REE------0", "Spain", listOf("ES")),
        BiddingZone("10YPT-REN------W", "Portugal", listOf("PT")),
        BiddingZone("10YNL----------L", "Netherlands", listOf("NL")),
        BiddingZone("10YBE----------2", "Belgium", listOf("BE")),
        BiddingZone("10YAT-APG------L", "Austria", listOf("AT")),
        BiddingZone("10YCH-SWISSGRIDZ", "Switzerland", listOf("CH")),
        BiddingZone("10YPL-AREA-----S", "Poland", listOf("PL")),
        BiddingZone("10YCZ-CEPS-----N", "Czech Republic", listOf("CZ")),
        BiddingZone("10YSK-SEPS-----K", "Slovakia", listOf("SK")),
        BiddingZone("10YHU-MAVIR----U", "Hungary", listOf("HU")),
        BiddingZone("10YRO-TEL------P", "Romania", listOf("RO")),
        BiddingZone("10YCA-BULGARIA-R", "Bulgaria", listOf("BG")),
        BiddingZone("10YHR-HEP------M", "Croatia", listOf("HR")),
        BiddingZone("10YSI-ELES-----O", "Slovenia", listOf("SI")),
        BiddingZone("10YGR-HTSO-----Y", "Greece", listOf("GR")),
        BiddingZone("10Y1001A1001A44P", "Sweden (SE1)", listOf("SE")),
        BiddingZone("10YFI-1--------U", "Finland", listOf("FI")),
        BiddingZone("10Y1001A1001A39I", "Estonia", listOf("EE")),
        BiddingZone("10YLV-1001A00074", "Latvia", listOf("LV")),
        BiddingZone("10YLT-1001A0008Q", "Lithuania", listOf("LT")),
        BiddingZone("10YDK-1--------W", "Denmark (DK1)", listOf("DK")),
        BiddingZone("10YIT-GRTN-----B", "Italy", listOf("IT")),
        BiddingZone("10YBA-JPCC-----D", "Bosnia & Herzegovina", listOf("BA")),
        BiddingZone("10YCS-SERBIATSOV", "Serbia", listOf("RS")),
        BiddingZone("10YCS-CG-TSO---S", "Montenegro", listOf("ME")),
        BiddingZone("10YMK-MEPSO----2", "North Macedonia", listOf("MK")),
        BiddingZone("10YAL-KESH-----5", "Albania", listOf("AL")),
        BiddingZone("10YNO-1--------2", "Norway (NO1)", listOf("NO")),
        BiddingZone("10Y1001A1001A016", "Northern Ireland", listOf("GB-NIR")),
        BiddingZone("10YGB----------A", "Great Britain", listOf("GB")),
        BiddingZone("10YIE-1001A00010", "Ireland", listOf("IE"))
    )

    fun forCountryCode(isoAlpha2: String): BiddingZone? =
        zones.find { zone -> zone.countryCodes.any { it.equals(isoAlpha2, ignoreCase = true) } }

    fun default(): BiddingZone = zones.first { it.countryCodes.contains("DE") }

    fun all(): List<BiddingZone> = zones
}
