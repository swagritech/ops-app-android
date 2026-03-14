package au.com.swagritech.opsapp.auth

import au.com.swagritech.opsapp.BuildConfig
import au.com.swagritech.opsapp.api.ApiClient
import java.net.URI
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object EasyAuthBridge {
    fun exchangeForSession(accessToken: String?, idToken: String?): Pair<String?, String?> {
        val url = buildEasyAuthLoginUrl() ?: return (null to "EasyAuth login URL could not be built")
        val client = ApiClient.authHttpClient
        val jsonType = "application/json; charset=utf-8".toMediaType()

        val diagnostics = mutableListOf<String>()
        val payloads = mutableListOf<JSONObject>()
        if (!accessToken.isNullOrBlank() || !idToken.isNullOrBlank()) {
            val combined = JSONObject()
            if (!accessToken.isNullOrBlank()) combined.put("access_token", accessToken)
            if (!idToken.isNullOrBlank()) combined.put("id_token", idToken)
            payloads.add(combined)
        }
        if (!accessToken.isNullOrBlank()) payloads.add(JSONObject().put("access_token", accessToken))
        if (!idToken.isNullOrBlank()) payloads.add(JSONObject().put("id_token", idToken))

        for (payload in payloads) {
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonType))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val token = runCatching {
                        JSONObject(body).optString("authenticationToken").ifBlank { null }
                    }.getOrNull()
                    if (!token.isNullOrBlank()) return token to null
                    if (ApiClient.hasSessionCookies()) return null to null
                    diagnostics.add("HTTP ${resp.code}: no authenticationToken in body and no EasyAuth session cookie")
                } else {
                    val body = resp.body?.string().orEmpty().take(200)
                    diagnostics.add("HTTP ${resp.code}: $body")
                }
            }
        }

        return null to diagnostics.joinToString(" | ").ifBlank { "EasyAuth exchange failed" }
    }

    private fun buildEasyAuthLoginUrl(): String? {
        return runCatching {
            val uri = URI(BuildConfig.API_BASE_URL)
            val scheme = uri.scheme ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port > 0) ":${uri.port}" else ""
            "$scheme://$host$port/.auth/login/aad"
        }.getOrNull()
    }
}

