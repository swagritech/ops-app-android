package au.com.swagritech.opsapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import au.com.swagritech.opsapp.BuildConfig
import au.com.swagritech.opsapp.MainActivity
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra("auth_cancelled", false) == true) {
            AuthSession.clear()
            returnToApp()
            return
        }

        val response = AuthorizationResponse.fromIntent(intent)
        val authException = AuthorizationException.fromIntent(intent)

        if (response == null) {
            AuthSession.clear()
            AuthSession.lastError = authException?.errorDescription ?: "Authorization response missing"
            returnToApp()
            return
        }

        val authService = AuthorizationService(this)
        val tokenRequest = response.createTokenExchangeRequest()

        authService.performTokenRequest(tokenRequest) { tokenResponse, tokenEx ->
            if (tokenResponse != null && !tokenResponse.accessToken.isNullOrBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    AuthSession.accessToken = tokenResponse.accessToken
                    AuthSession.refreshToken = tokenResponse.refreshToken
                    AuthSession.expiresAtSeconds = tokenResponse.accessTokenExpirationTime?.div(1000L)
                    AuthSession.username = tokenResponse.idToken?.let { extractPilotDisplayFromIdToken(it) }
                        ?: tokenResponse.additionalParameters["name"]
                        ?: tokenResponse.additionalParameters["preferred_username"]
                        ?: tokenResponse.additionalParameters["upn"]
                        ?: "Microsoft User"

                    // Exchange provider token for EasyAuth token used by Azure App Service auth pipeline.
                    val easyAuth = exchangeForEasyAuthToken(
                        accessToken = tokenResponse.accessToken,
                        idToken = tokenResponse.idToken
                    )
                    AuthSession.easyAuthToken = easyAuth

                    if (easyAuth.isNullOrBlank()) {
                        AuthSession.lastError = "Signed in, but EasyAuth session token was not returned"
                    } else {
                        AuthSession.lastError = null
                    }

                    AuthStore.save(
                        this@AuthCallbackActivity,
                        PersistedAuth(
                            accessToken = AuthSession.accessToken ?: "",
                            refreshToken = AuthSession.refreshToken,
                            username = AuthSession.username,
                            expiresAtSeconds = AuthSession.expiresAtSeconds,
                            easyAuthToken = AuthSession.easyAuthToken
                        )
                    )

                    withContext(Dispatchers.Main) {
                        returnToApp()
                    }
                }
            } else {
                AuthSession.clear()
                AuthSession.lastError = tokenEx?.errorDescription ?: tokenEx?.message ?: "Token exchange failed"
                AuthStore.clear(this)
                returnToApp()
            }
        }
    }

    private fun returnToApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }

    private fun extractPilotDisplayFromIdToken(idToken: String): String? {
        return runCatching {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val json = JSONObject(payload)
            json.optString("name")
                .ifBlank { json.optString("preferred_username") }
                .ifBlank { json.optString("upn") }
                .ifBlank { json.optString("email") }
                .ifBlank { null }
        }.getOrNull()
    }

    private fun exchangeForEasyAuthToken(accessToken: String?, idToken: String?): String? {
        val url = buildEasyAuthLoginUrl() ?: return null
        val client = OkHttpClient()
        val jsonType = "application/json; charset=utf-8".toMediaType()

        val payloads = mutableListOf<JSONObject>()
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
                    if (!token.isNullOrBlank()) return token
                }
            }
        }

        return null
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
