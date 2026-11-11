package com.erw.kickthecan.calendar

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Minimal blocking client for the Google Calendar v3 REST API (primary calendar). */
class CalendarApi {

    class AuthException(message: String) : IOException(message)

    /** Times are null for all-day events. */
    data class RemoteEvent(val startMillis: Long?, val endMillis: Long?)

    private class Response(val code: Int, val body: String)

    fun insert(token: String, title: String, startMillis: Long, endMillis: Long, localId: String): String {
        val body = JSONObject()
            .put("summary", TITLE_TAG + title)
            .put("start", timeJson(startMillis))
            .put("end", timeJson(endMillis))
            .put(
                "extendedProperties",
                JSONObject().put("private", JSONObject().put("kickTheCanId", localId))
            )
        val response = request("POST", EVENTS_URL, token, body)
        check(response, 200)
        return JSONObject(response.body).getString("id")
    }

    /** Returns false if the event no longer exists remotely. */
    fun move(token: String, googleId: String, startMillis: Long, endMillis: Long): Boolean {
        val body = JSONObject().put("start", timeJson(startMillis)).put("end", timeJson(endMillis))
        val response = request("PATCH", "$EVENTS_URL/$googleId", token, body)
        if (response.code == 404 || response.code == 410) return false
        check(response, 200)
        return true
    }

    fun delete(token: String, googleId: String) {
        val response = request("DELETE", "$EVENTS_URL/$googleId", token, null)
        if (response.code == 404 || response.code == 410) return
        check(response, 204, 200)
    }

    /** Returns null if the event was deleted or cancelled remotely. */
    fun get(token: String, googleId: String): RemoteEvent? {
        val response = request("GET", "$EVENTS_URL/$googleId", token, null)
        if (response.code == 404 || response.code == 410) return null
        check(response, 200)
        val json = JSONObject(response.body)
        if (json.optString("status") == "cancelled") return null
        return RemoteEvent(
            startMillis = json.optJSONObject("start")?.let(::parseTime),
            endMillis = json.optJSONObject("end")?.let(::parseTime),
        )
    }

    private fun check(response: Response, vararg okCodes: Int) {
        if (response.code in okCodes) return
        if (response.code == 401) throw AuthException("Unauthorized")
        throw IOException("Calendar API error ${response.code}: ${response.body.take(200)}")
    }

    private fun request(method: String, url: String, token: String, body: JSONObject?): Response {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            // HttpURLConnection has no PATCH; Google honours the override header.
            conn.requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code < 400) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Response(code, text)
        } finally {
            conn.disconnect()
        }
    }

    private fun timeJson(millis: Long) = JSONObject().put("dateTime", format().format(millis))

    private fun parseTime(json: JSONObject): Long? =
        json.optString("dateTime").takeIf { it.isNotEmpty() }?.let {
            runCatching { format().parse(it)?.time }.getOrNull()
        }

    // SimpleDateFormat isn't thread-safe, so create one per use.
    private fun format() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private companion object {
        const val EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
        const val TITLE_TAG = "[KtC] "
        const val TIMEOUT_MS = 15_000
    }
}
