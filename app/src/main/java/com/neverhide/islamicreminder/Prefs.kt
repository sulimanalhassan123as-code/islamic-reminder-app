package com.neverhide.islamicreminder

import android.content.Context

object Prefs {
    private const val FILE = "prefs"

    const val LAT = "lat"
    const val LNG = "lng"
    const val FAJR_ANGLE = "fajr_angle"
    const val ISHA_ANGLE = "isha_angle"
    const val ASR_FACTOR = "asr_factor"
    const val AZAN_ON = "azan_on"
    const val DAILY_ON = "daily_on"
    const val FRIDAY_ON = "friday_on"
    const val SETUP_DONE = "setup_done"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun get(ctx: Context, key: String, def: Double): Double =
        sp(ctx).getFloat(key, def.toFloat()).toDouble()

    fun get(ctx: Context, key: String, def: Boolean): Boolean = sp(ctx).getBoolean(key, def)

    fun get(ctx: Context, key: String, def: Long): Long = sp(ctx).getLong(key, def)

    fun get(ctx: Context, key: String, def: String): String = sp(ctx).getString(key, def) ?: def

    fun set(ctx: Context, key: String, v: Double) = sp(ctx).edit().putFloat(key, v.toFloat()).apply()

    fun set(ctx: Context, key: String, v: Boolean) = sp(ctx).edit().putBoolean(key, v).apply()

    fun set(ctx: Context, key: String, v: Long) = sp(ctx).edit().putLong(key, v).apply()

    fun set(ctx: Context, key: String, v: String) = sp(ctx).edit().putString(key, v).apply()
}
