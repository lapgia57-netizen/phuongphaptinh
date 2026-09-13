package com.multipay.pro

import java.text.NumberFormat
import java.util.Locale

object Utils {

    private val vnFormat = NumberFormat.getInstance(Locale("vi", "VN"))

    fun formatVND(amount: Double): String {
        return "${vnFormat.format(amount.toLong())} ₫"
    }

    fun formatDateVN(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "—"
        val parts = dateStr.split("-")
        return when (parts.size) {
            3 -> "${parts[2]}/${parts[1]}/${parts[0]}"
            2 -> "Tháng ${parts[1]}/${parts[0]}"
            else -> dateStr
        }
    }
}
