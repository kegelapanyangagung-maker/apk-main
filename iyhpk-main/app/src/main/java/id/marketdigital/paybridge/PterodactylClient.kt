package id.marketdigital.paybridge

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PterodactylClient(
    private val panelUrl: String,
    private val apiKey: String,
    private val serverId: String
) {
    data class Result(val ok: Boolean, val code: Int, val message: String)

    private fun connection(path: String, method: String): HttpURLConnection {
        val base = panelUrl.trim().trimEnd('/')
        val url = URL("$base$path")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10000
            readTimeout = 15000
            doInput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "Application/vnd.pterodactyl.v1+json")
            setRequestProperty("Content-Type", "application/json")
        }
    }

    fun testConnection(): Result {
        if (!panelUrl.startsWith("https://")) return Result(false, 0, "Panel harus menggunakan HTTPS")
        if (apiKey.isBlank() || serverId.isBlank()) return Result(false, 0, "API key dan Server ID wajib diisi")
        return try {
            val conn = connection("/api/client/servers/$serverId", "GET")
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
            conn.disconnect()
            if (code in 200..299) {
                val name = runCatching { JSONObject(body).getJSONObject("attributes").optString("name") }.getOrDefault(serverId)
                Result(true, code, "Terhubung ke server: $name")
            } else {
                Result(false, code, "Gagal terhubung (HTTP $code)")
            }
        } catch (e: Exception) {
            Result(false, 0, "Koneksi gagal: ${e.message ?: "unknown error"}")
        }
    }

    fun power(signal: String): Result {
        if (!panelUrl.startsWith("https://")) return Result(false, 0, "Panel harus menggunakan HTTPS")
        if (apiKey.isBlank() || serverId.isBlank()) return Result(false, 0, "API key dan Server ID wajib diisi")
        return try {
            val conn = connection("/api/client/servers/$serverId/power", "POST")
            conn.doOutput = true
            conn.outputStream.use { it.write(JSONObject().put("signal", signal).toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) Result(true, code, "Perintah $signal berhasil dikirim")
            else Result(false, code, "Perintah ditolak (HTTP $code)")
        } catch (e: Exception) {
            Result(false, 0, "Gagal mengirim perintah: ${e.message ?: "unknown error"}")
        }
    }
}
