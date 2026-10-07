package com.whatsapptoexcel.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Types of structured report lines in Field Work.
 */
enum class ReportLineType {
    APPLICANT,
    MALNAR,
    FAMILY,
    HOME_TYPE,
    RESIDENCE,
    HOME_OWNERSHIP,
    RENT,
    CUSTOM
}

/**
 * Structured report line item representing a dynamic, renumberable row.
 */
data class ReportLineItem(
    val id: String = UUID.randomUUID().toString(),
    var type: ReportLineType = ReportLineType.CUSTOM,
    var text: String = "",
    var extraData: String = ""
)

/**
 * Data model for preserving the Field Work report template draft
 * so user inputs are never lost when the floating popup is closed or reopened.
 */
data class ReportDraft(
    var rvValue: String = "",
    var applicantName: String = "",
    var malnar: String = "Self",
    var familyTotal: String = "",
    var familyEarning: String = "",
    var homeType: String = "Home Tenement",
    var residenceYears: String = "",
    var isHomeRent: Boolean = false, // false = "Home potanu chhe", true = "Home rent par chhe"
    var rentAmount: String = "", // e.g. "5000"
    var homeOwnershipMode: Int = 0, // kept for backward compatibility
    var homeOwnershipCustom: String = "",
    var extraInfo: String = "",
    var tpc: String = "",
    var locationName: String = "",
    var latitude: String = "",
    var longitude: String = ""
)

/**
 * Repository to persist and restore the active report draft using SharedPreferences.
 */
class ReportDraftRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadDraft(): ReportDraft {
        return ReportDraft(
            rvValue = prefs.getString(KEY_RV, "") ?: "",
            applicantName = prefs.getString(KEY_APPLICANT, "") ?: "",
            malnar = prefs.getString(KEY_MALNAR, "Self") ?: "Self",
            familyTotal = prefs.getString(KEY_FAMILY_TOTAL, "") ?: "",
            familyEarning = prefs.getString(KEY_FAMILY_EARNING, "") ?: "",
            homeType = prefs.getString(KEY_HOME_TYPE, "Home Tenement") ?: "Home Tenement",
            residenceYears = prefs.getString(KEY_RESIDENCE_YEARS, "") ?: "",
            isHomeRent = prefs.getBoolean(KEY_IS_HOME_RENT, false),
            rentAmount = prefs.getString(KEY_RENT_AMOUNT, "") ?: "",
            homeOwnershipMode = prefs.getInt(KEY_HOME_OWNERSHIP_MODE, 0),
            homeOwnershipCustom = prefs.getString(KEY_HOME_OWNERSHIP_CUSTOM, "") ?: "",
            extraInfo = prefs.getString(KEY_EXTRA_INFO, "") ?: "",
            tpc = prefs.getString(KEY_TPC, "") ?: "",
            locationName = prefs.getString(KEY_LOCATION_NAME, "") ?: "",
            latitude = prefs.getString(KEY_LATITUDE, "") ?: "",
            longitude = prefs.getString(KEY_LONGITUDE, "") ?: ""
        )
    }

    fun saveDraft(draft: ReportDraft) {
        prefs.edit().apply {
            putString(KEY_RV, draft.rvValue)
            putString(KEY_APPLICANT, draft.applicantName)
            putString(KEY_MALNAR, draft.malnar)
            putString(KEY_FAMILY_TOTAL, draft.familyTotal)
            putString(KEY_FAMILY_EARNING, draft.familyEarning)
            putString(KEY_HOME_TYPE, draft.homeType)
            putString(KEY_RESIDENCE_YEARS, draft.residenceYears)
            putBoolean(KEY_IS_HOME_RENT, draft.isHomeRent)
            putString(KEY_RENT_AMOUNT, draft.rentAmount)
            putInt(KEY_HOME_OWNERSHIP_MODE, draft.homeOwnershipMode)
            putString(KEY_HOME_OWNERSHIP_CUSTOM, draft.homeOwnershipCustom)
            putString(KEY_EXTRA_INFO, draft.extraInfo)
            putString(KEY_TPC, draft.tpc)
            putString(KEY_LOCATION_NAME, draft.locationName)
            putString(KEY_LATITUDE, draft.latitude)
            putString(KEY_LONGITUDE, draft.longitude)
            apply()
        }
    }

    fun clearDraft() {
        prefs.edit().clear().apply()
    }

    /**
     * Loads the saved floating popup opacity level (Default: 0.95f / 95% opacity).
     */
    fun loadOpacity(): Float {
        return prefs.getFloat(KEY_OPACITY, 0.95f)
    }

    /**
     * Saves the user-selected floating popup opacity level.
     */
    fun saveOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_OPACITY, opacity).apply()
    }

    /**
     * Loads the list of structured report lines.
     * If no customized lines were saved, generates the initial standard 7 rows.
     */
    fun loadReportLines(draft: ReportDraft): MutableList<ReportLineItem> {
        val json = prefs.getString(KEY_REPORT_LINES, "") ?: ""
        if (json.isNotEmpty()) {
            val list = mutableListOf<ReportLineItem>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val typeStr = obj.optString("type", ReportLineType.CUSTOM.name)
                    val type = try {
                        ReportLineType.valueOf(typeStr)
                    } catch (e: Exception) {
                        ReportLineType.CUSTOM
                    }
                    val text = obj.optString("text", "")
                    val extraData = obj.optString("extraData", "")
                    list.add(ReportLineItem(id = id, type = type, text = text, extraData = extraData))
                }
                if (list.isNotEmpty()) {
                    return list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return createDefaultLines(draft).toMutableList()
    }

    /**
     * Saves the structured report lines to SharedPreferences.
     */
    fun saveReportLines(lines: List<ReportLineItem>) {
        try {
            val array = JSONArray()
            for (line in lines) {
                val obj = JSONObject().apply {
                    put("id", line.id)
                    put("type", line.type.name)
                    put("text", line.text)
                    put("extraData", line.extraData)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_REPORT_LINES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDefaultLines(draft: ReportDraft): List<ReportLineItem> {
        val list = mutableListOf<ReportLineItem>()
        list.add(ReportLineItem(id = "1", type = ReportLineType.APPLICANT, text = draft.applicantName))
        list.add(ReportLineItem(id = "2", type = ReportLineType.MALNAR, text = if (draft.malnar.isEmpty()) "Self" else draft.malnar))
        list.add(ReportLineItem(id = "3", type = ReportLineType.FAMILY, text = draft.familyTotal, extraData = draft.familyEarning))
        list.add(ReportLineItem(id = "4", type = ReportLineType.HOME_TYPE, text = if (draft.homeType.isEmpty()) "Home Tenement" else draft.homeType))
        list.add(ReportLineItem(id = "5", type = ReportLineType.RESIDENCE, text = draft.residenceYears))
        list.add(ReportLineItem(id = "6", type = ReportLineType.HOME_OWNERSHIP, text = if (draft.isHomeRent) "Home rent par chhe" else "Home potanu chhe"))
        if (draft.isHomeRent) {
            list.add(ReportLineItem(id = "7", type = ReportLineType.RENT, text = draft.rentAmount))
        } else {
            list.add(ReportLineItem(id = "7", type = ReportLineType.CUSTOM, text = draft.extraInfo))
        }
        return list
    }

    /**
     * Formats the entire report into a clean, standard Field Work text report with dynamic numbering.
     * Skips blank custom lines (such as a blank 7th line) so they don't produce empty numbered rows.
     */
    fun formatReportText(draft: ReportDraft, lines: List<ReportLineItem>, gpsText: String = ""): String {
        val sb = StringBuilder()

        // Header: R.V (Value)
        val rv = draft.rvValue.trim()
        sb.append("R.V (").append(rv).append(")\n\n")

        // Filter out blank custom lines (e.g. line 7 when blank)
        val activeLines = lines.filter { line ->
            if (line.type == ReportLineType.CUSTOM) {
                line.text.trim().isNotEmpty()
            } else {
                true
            }
        }

        // Dynamic numbered rows
        activeLines.forEachIndexed { index, line ->
            val num = index + 1
            sb.append(num).append(") ")
            when (line.type) {
                ReportLineType.APPLICANT -> {
                    val app = line.text.trim()
                    sb.append("Applicant:- ").append(app)
                }
                ReportLineType.MALNAR -> {
                    val mal = line.text.trim().ifEmpty { "Self" }
                    sb.append("Malnar:- ").append(mal)
                }
                ReportLineType.FAMILY -> {
                    val tot = line.text.trim()
                    val earn = line.extraData.trim()
                    val totFormatted = if (tot.length == 1) "0$tot" else tot
                    val earnFormatted = if (earn.length == 1) "0$earn" else earn
                    sb.append("Family:- ").append(totFormatted).append("_").append(earnFormatted)
                }
                ReportLineType.HOME_TYPE -> {
                    val ht = line.text.trim().ifEmpty { "Home Tenement" }
                    sb.append(ht)
                }
                ReportLineType.RESIDENCE -> {
                    val yrs = line.text.trim()
                    sb.append(yrs).append(" year thi ahiya rahe chhe")
                }
                ReportLineType.HOME_OWNERSHIP -> {
                    val ho = line.text.trim().ifEmpty { "Home potanu chhe" }
                    sb.append(ho)
                }
                ReportLineType.RENT -> {
                    val rent = line.text.trim()
                    sb.append("rent:- ").append(rent).append(" rs per month")
                }
                ReportLineType.CUSTOM -> {
                    sb.append(line.text.trim())
                }
            }
            sb.append("\n")
        }

        // TPC Line
        val tpc = draft.tpc.trim()
        if (tpc.isNotEmpty()) {
            sb.append("\nTPC. ").append(tpc).append("\n\n")
        } else {
            sb.append("\nTPC.\n\n")
        }

        // # Location & GPS
        val loc = draft.locationName.trim()
        var lat = draft.latitude.trim()
        var lon = draft.longitude.trim()

        if ((lat.isEmpty() || lon.isEmpty()) && gpsText.isNotBlank()) {
            val clean = gpsText.replace("[", "").replace("]", "").trim()
            val parts = clean.split(",")
            if (parts.size >= 2) {
                lat = parts[0].trim()
                lon = parts[1].trim()
            }
        }

        val hasGps = lat.isNotEmpty() && lon.isNotEmpty()
        val gpsCoordStr = if (hasGps) "$lat,$lon" else ""

        sb.append("# ")
        if (loc.isNotEmpty() && hasGps) {
            sb.append(loc).append(" ").append(gpsCoordStr)
        } else if (loc.isNotEmpty()) {
            sb.append(loc)
        } else if (hasGps) {
            sb.append(gpsCoordStr)
        } else {
            sb.append("____________   ____________")
        }

        return sb.toString()
    }

    companion object {
        private const val PREFS_NAME = "field_work_report_draft"
        private const val KEY_REPORT_LINES = "report_lines_json"
        private const val KEY_OPACITY = "popup_opacity"
        private const val KEY_RV = "rv_value"
        private const val KEY_APPLICANT = "applicant_name"
        private const val KEY_MALNAR = "malnar"
        private const val KEY_FAMILY_TOTAL = "family_total"
        private const val KEY_FAMILY_EARNING = "family_earning"
        private const val KEY_HOME_TYPE = "home_type"
        private const val KEY_RESIDENCE_YEARS = "residence_years"
        private const val KEY_IS_HOME_RENT = "is_home_rent"
        private const val KEY_RENT_AMOUNT = "rent_amount"
        private const val KEY_HOME_OWNERSHIP_MODE = "home_ownership_mode"
        private const val KEY_HOME_OWNERSHIP_CUSTOM = "home_ownership_custom"
        private const val KEY_EXTRA_INFO = "extra_info"
        private const val KEY_TPC = "tpc"
        private const val KEY_LOCATION_NAME = "location_name"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }
}
