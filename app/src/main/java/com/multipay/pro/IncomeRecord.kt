package com.multipay.pro

data class IncomeRecord(
    var id: String = "",
    var jobTitle: String = "",
    var type: String = "MONTHLY", // MONTHLY | PARTTIME
    var note: String = "",
    var createdAt: Long = 0L,

    // shared
    var timeStr: String = "",   // "yyyy-MM" for monthly, "yyyy-MM-dd" for parttime
    var monthKey: String = "",  // "yyyy-MM"
    var total: Double = 0.0,
    var detailText: String = "",

    // monthly-only
    var baseIncome: Double = 0.0,
    var extraIncome: Double = 0.0,

    // parttime-only
    var unit: String = "HOURLY", // HOURLY | PRODUCT
    var qty: Double = 0.0,
    var rate: Double = 0.0
) {
    // Required no-arg mapping helper for Firebase (data class already has defaults)
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "jobTitle" to jobTitle,
            "type" to type,
            "note" to note,
            "createdAt" to createdAt,
            "timeStr" to timeStr,
            "monthKey" to monthKey,
            "total" to total,
            "detailText" to detailText,
            "baseIncome" to baseIncome,
            "extraIncome" to extraIncome,
            "unit" to unit,
            "qty" to qty,
            "rate" to rate
        )
    }
}
