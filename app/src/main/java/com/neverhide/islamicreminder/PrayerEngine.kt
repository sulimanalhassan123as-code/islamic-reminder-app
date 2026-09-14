package com.neverhide.islamicreminder

import java.util.Calendar
import kotlin.math.abs

/**
 * Local prayer time engine — port of the well-known PrayTimes.org algorithm.
 * Works fully offline; no API needed.
 */
object PrayerEngine {

    data class Times(
        val fajr: Double, val sunrise: Double, val dhuhr: Double,
        val asr: Double, val maghrib: Double, val isha: Double
    )

    // Calculation methods: name -> (fajrAngle, ishaAngle)
    val METHODS = linkedMapOf(
        "MWL (18°/17°)" to (18.0 to 17.0),
        "ISNA (15°/15°)" to (15.0 to 15.0),
        "Egypt (19.5°/17.5°)" to (19.5 to 17.5),
        "Umm al-Qura (18.5°/90min)" to (18.5 to 18.5)
    )

    fun compute(cal: Calendar, lat: Double, lng: Double, tzHours: Double,
                fajrAngle: Double, ishaAngle: Double, asrFactor: Double = 1.0): Times {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val jDate = julian(y, m, d) - lng / (15.0 * 24.0)

        // Sun position
        val D = jDate - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * D)
        val q = fixAngle(280.459 + 0.98560047 * D)
        val L = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * D
        val RA = dtan2(dcos(e) * dsin(L), dcos(L)) / 15.0
        val eqt = q / 15.0 - fixHour(RA)
        val decl = darcsin(dsin(e) * dsin(L))

        val noon = fixHour(12.0 - eqt) - lng / 15.0 + tzHours

        fun t(angleDeg: Double): Double {
            val num = -dsin(angleDeg) - dsin(decl) * dsin(lat)
            val den = dcos(decl) * dcos(lat)
            val x = if (den == 0.0) 0.0 else num / den
            val clamped = x.coerceIn(-1.0, 1.0)
            return darccos(clamped) / 15.0
        }

        val fajr = noon - t(fajrAngle)
        val sunrise = noon - t(0.833)
        val maghrib = noon + t(0.833)
        val isha = noon + t(ishaAngle)
        val asrAngle = -darccot(asrFactor + dtan(abs(lat - decl)))
        val asr = noon + t(asrAngle)

        return Times(fajr, sunrise, noon, asr, maghrib, isha)
    }

    /** Returns today's times + tomorrow's Fajr for the countdown. */
    fun todayTimes(lat: Double, lng: Double, tzHours: Double,
                   fajrAngle: Double, ishaAngle: Double, asrFactor: Double = 1.0): Pair<Times, Times> {
        val now = Calendar.getInstance()
        val today = compute(now, lat, lng, tzHours, fajrAngle, ishaAngle, asrFactor)
        val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrow = compute(tomorrowCal, lat, lng, tzHours, fajrAngle, ishaAngle, asrFactor)
        return today to tomorrow
    }

    fun format12(hours: Double): String {
        if (hours.isNaN() || hours < 0.0 || hours >= 100.0) return "--:--"
        var total = Math.round(hours * 60.0)
        if (total >= 24 * 60) total -= 24 * 60
        var h = (total / 60).toInt()
        val m = (total % 60).toInt()
        val ampm = if (h >= 12) "PM" else "AM"
        h = h % 12
        if (h == 0) h = 12
        return String.format("%d:%02d %s", h, m, ampm)
    }

    private fun julian(y: Int, m: Int, d: Int): Double {
        var yy = y
        var mm = m
        if (mm <= 2) { yy -= 1; mm += 12 }
        val a = yy / 100
        val b = 2 - a + a / 4
        return Math.floor(365.25 * (yy + 4716)) + Math.floor(30.6001 * (mm + 1)) + d + b - 1524.5
    }

    private fun dsin(x: Double) = Math.sin(Math.toRadians(x))
    private fun dcos(x: Double) = Math.cos(Math.toRadians(x))
    private fun dtan(x: Double) = Math.tan(Math.toRadians(x))
    private fun darcsin(x: Double) = Math.toDegrees(kotlin.math.asin(x))
    private fun darccos(x: Double) = Math.toDegrees(kotlin.math.acos(x))
    private fun dtan2(y: Double, x: Double) = Math.toDegrees(Math.atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(Math.atan(1.0 / x))

    private fun fixAngle(a: Double): Double {
        var x = a % 360.0
        if (x < 0) x += 360.0
        return x
    }

    private fun fixHour(h: Double): Double {
        var x = h % 24.0
        if (x < 0) x += 24.0
        return x
    }
}
