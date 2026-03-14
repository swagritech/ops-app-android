package au.com.swagritech.opsapp.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import au.com.swagritech.opsapp.MainActivity
import org.json.JSONObject
import android.util.Base64
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

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
                AuthSession.accessToken = tokenResponse.accessToken
                AuthSession.refreshToken = tokenResponse.refreshToken
                AuthSession.expiresAtSeconds = tokenResponse.accessTokenExpirationTime?.div(1000L)
                AuthSession.username = tokenResponse.additionalParameters["preferred_username"]
                    ?: tokenResponse.additionalParameters["upn"]
                    ?: tokenResponse.idToken?.let { extractPreferredUsernameFromIdToken(it) }
                    ?: "Microsoft User"
                AuthSession.lastError = null
                AuthStore.save(
                    this,
                    PersistedAuth(
                        accessToken = AuthSession.accessToken ?: "",
                        refreshToken = AuthSession.refreshToken,
                        username = AuthSession.username,
                        expiresAtSeconds = AuthSession.expiresAtSeconds
                    )
                )
            } else {
                AuthSession.clear()
                AuthSession.lastError = tokenEx?.errorDescription ?: tokenEx?.message ?: "Token exchange failed"
                AuthStore.clear(this)
            }
            returnToApp()
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

    private fun extractPreferredUsernameFromIdToken(idToken: String): String? {
        return runCatching {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val json = JSONObject(payload)
            json.optString("preferred_username")
                .ifBlank { json.optString("upn") }
                .ifBlank { json.optString("email") }
                .ifBlank { json.optString("name") }
                .ifBlank { null }
        }.getOrNull()
    }
}
