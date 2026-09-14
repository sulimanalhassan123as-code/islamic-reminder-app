package com.neverhide.islamicreminder

import android.content.Context
import java.io.DataInputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Minimal Supabase REST client — zero dependencies, works on any Android.
 * Registers the device (user count for the admin panel) and polls for
 * admin broadcasts.
 */
object SupabaseApi {

    private const val BASE = "https://hokqlvkowcrujppeliip.supabase.co/rest/v1"
    private val ANON: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhva3Fsdmtvd2NydWpwcGVsaWlwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk3MTg1MDUsImV4cCI6MjA5NTI5NDUwNX0._iO6p71kJRiBWH-fJ1j7GWDNmMcjSMN5nseNU4VN8tE"

    fun deviceId(ctx: Context): String {
        var id = Prefs.get(ctx, "device_id", "")
        if (id.isBlank()) {
            id = UUID.randomUUID().toString()
            Prefs.set(ctx, "device_id", id)
        }
        return id
    }

    fun registerAsync(ctx: Context) {
        Thread {
            try {
                val body = """
                    {"device_id":"${deviceId(ctx)}","app_version":"${BuildInfo.VERSION}","city":"${BuildInfo.CITY}"}
                """.trim().replace("\n", "")
                post("/islamic_app_users?on_conflict=device_id", body, "resolution=merge-duplicates")
                Prefs.set(ctx, "registered", true)
            } catch (e: Exception) {
                // Offline — heartbeat will retry
            }
        }.start()
    }

    fun syncAsync(ctx: Context) {
        Thread {
            try {
                // Update last_seen
                val seen = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date()) + "Z"
                patch("/islamic_app_users?device_id=eq.${deviceId(ctx)}",
                    "{\"last_seen\":\"$seen\",\"app_version\":\"${BuildInfo.VERSION}\"}")

                // Poll broadcasts
                val json = get("/islamic_broadcasts?order=id.desc&limit=3")
                val lastSeenId = Prefs.get(ctx, "last_broadcast", 0L)
                val ids = parseBroadcastIds(json)
                val fresh = ids.filter { it > lastSeenId }.sorted()
                for (id in fresh) {
                    val (title, body) = parseBroadcast(json, id)
                    if (title != null) {
                        Notifications.showAdminBroadcast(ctx, id, title, body ?: "")
                    }
                }
                if (ids.isNotEmpty()) Prefs.set(ctx, "last_broadcast", ids.max())
            } catch (e: Exception) {
                // Silent — next heartbeat retries
            }
        }.start()
    }

    // ---------- HTTP plumbing ----------

    private fun http(path: String, method: String, body: String?, prefer: String?): String {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("apikey", ANON)
        conn.setRequestProperty("Authorization", "Bearer $ANON")
        conn.setRequestProperty("Content-Type", "application/json")
        if (prefer != null) conn.setRequestProperty("Prefer", prefer)
        if (body != null) {
            conn.doOutput = true
            val w = OutputStreamWriter(conn.outputStream)
            w.write(body)
            w.flush()
            w.close()
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val bytes = stream?.readBytes() ?: ByteArray(0)
        conn.disconnect()
        if (code !in 200..299) throw RuntimeException("HTTP $code")
        return String(bytes)
    }

    private fun get(path: String) = http(path, "GET", null, null)
    private fun post(path: String, body: String, prefer: String) = http(path, "POST", body, prefer)
    private fun patch(path: String, body: String) = http(path, "PATCH", body, null)

    // ---------- Tiny JSON extractors (no JSON library needed) ----------

    private fun parseBroadcastIds(json: String): List<Long> {
        val ids = mutableListOf<Long>()
        var i = 0
        while (true) {
            i = json.indexOf("\"id\":", i)
            if (i < 0) break
            var j = i + 5
            var num = ""
            while (j < json.length && (json[j].isDigit())) { num += json[j]; j++ }
            if (num.isNotEmpty()) ids.add(num.toLong())
            i = j
        }
        return ids.distinct()
    }

    private fun parseBroadcast(json: String, id: Long): Pair<String?, String?> {
        // Split into objects and find the one with this id
        val objects = json.split("},")
        for (obj in objects) {
            if (obj.contains("\"id\":$id,")) {
                val title = extractString(obj, "title")
                val body = extractString(obj, "body")
                return title to body
            }
        }
        return null to null
    }

    private fun extractString(json: String, key: String): String? {
        val idx = json.indexOf("\"$key\":")
        if (idx < 0) return null
        var i = idx + key.length + 3
        if (i >= json.length || json[i] != '"') return null
        i++
        val sb = StringBuilder()
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && i + 1 < json.length) {
                when (json[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    else -> sb.append(json[i + 1])
                }
                i += 2
            } else if (c == '"') break
            else { sb.append(c); i++ }
        }
        return sb.toString()
    }
}

object BuildInfo {
    const val VERSION = "1.0"
    const val CITY = "Accra"
}
