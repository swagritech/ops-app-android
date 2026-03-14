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

        if (idToken.isNullOrBlank()) {
            return null to "Session expired. Sign in with Microsoft again."
        }

        val payload = JSONObject().put("id_token", idToken)
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
                return null to "Signed in, but API session was not established."
            }
            return null to "Sign-in accepted, but API session could not be established (HTTP ${resp.code})."
        }
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
