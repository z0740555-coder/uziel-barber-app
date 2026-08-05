package com.uziel.barber

import android.icu.util.Calendar as IcuCalendar
import android.icu.util.HebrewCalendar
import java.util.Calendar

object HebrewDateHelper {

    private val hebrewMonthNames = arrayOf(
        "תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר א'", "אדר",
        "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול"
    )

    private val hebrewDayNumerals = arrayOf(
        "", "א'", "ב'", "ג'", "ד'", "ה'", "ו'", "ז'", "ח'", "ט'", "י'",
        "י\"א", "י\"ב", "י\"ג", "י\"ד", "ט\"ו", "ט\"ז", "י\"ז", "י\"ח", "י\"ט", "כ'",
        "כ\"א", "כ\"ב", "כ\"ג", "כ\"ד", "כ\"ה", "כ\"ו", "כ\"ז", "כ\"ח", "כ\"ט", "ל'"
    )

    private data class Holiday(val month: Int, val day: Int, val name: String)

    private val fixedHolidays = listOf(
        Holiday(HebrewCalendar.TISHRI, 1, "ראש השנה"),
        Holiday(HebrewCalendar.TISHRI, 2, "ראש השנה"),
        Holiday(HebrewCalendar.TISHRI, 10, "יום כיפור"),
        Holiday(HebrewCalendar.TISHRI, 15, "סוכות"),
        Holiday(HebrewCalendar.TISHRI, 16, "סוכות"),
        Holiday(HebrewCalendar.TISHRI, 17, "חול המועד סוכות"),
        Holiday(HebrewCalendar.TISHRI, 18, "חול המועד סוכות"),
        Holiday(HebrewCalendar.TISHRI, 19, "חול המועד סוכות"),
        Holiday(HebrewCalendar.TISHRI, 20, "חול המועד סוכות"),
        Holiday(HebrewCalendar.TISHRI, 21, "הושענא רבה"),
        Holiday(HebrewCalendar.TISHRI, 22, "שמיני עצרת / שמחת תורה"),
        Holiday(HebrewCalendar.ADAR, 14, "פורים"),
        Holiday(HebrewCalendar.NISAN, 15, "פסח"),
        Holiday(HebrewCalendar.NISAN, 16, "פסח"),
        Holiday(HebrewCalendar.NISAN, 17, "חול המועד פסח"),
        Holiday(HebrewCalendar.NISAN, 18, "חול המועד פסח"),
        Holiday(HebrewCalendar.NISAN, 19, "חול המועד פסח"),
        Holiday(HebrewCalendar.NISAN, 20, "חול המועד פסח"),
        Holiday(HebrewCalendar.NISAN, 21, "שביעי של פסח"),
        Holiday(HebrewCalendar.IYAR, 18, "ל\"ג בעומר"),
        Holiday(HebrewCalendar.SIVAN, 6, "שבועות"),
        Holiday(HebrewCalendar.TAMUZ, 17, "צום י\"ז בתמוז"),
        Holiday(HebrewCalendar.AV, 9, "תשעה באב")
    )

    private fun toHebrewCalendar(cal: Calendar): HebrewCalendar {
        val hc = HebrewCalendar()
        hc.timeInMillis = cal.timeInMillis
        return hc
    }

    fun hebrewDateString(cal: Calendar): String {
        val hc = toHebrewCalendar(cal)
        val day = hc.get(IcuCalendar.DAY_OF_MONTH)
        val month = hc.get(IcuCalendar.MONTH)
        val year = hc.get(IcuCalendar.YEAR)
        val dayStr = hebrewDayNumerals.getOrElse(day) { day.toString() }
        val monthStr = hebrewMonthNames.getOrElse(month) { "" }
        return "$dayStr ב$monthStr $year"
    }

    fun holidayName(cal: Calendar): String? {
        val hc = toHebrewCalendar(cal)
        val month = hc.get(IcuCalendar.MONTH)
        val day = hc.get(IcuCalendar.DAY_OF_MONTH)

        // Chanukah spans Kislev 25 through Tevet 2 or 3 (Kislev length varies)
        if (month == HebrewCalendar.KISLEV && day >= 25) return "חנוכה"
        if (month == HebrewCalendar.TEVET && day <= 3) return "חנוכה"

        return fixedHolidays.firstOrNull { it.month == month && it.day == day }?.name
    }

    fun isShabbat(cal: Calendar): Boolean {
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    }
}
