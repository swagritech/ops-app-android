package au.com.swagritech.opsapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import au.com.swagritech.opsapp.api.ApiClient
import au.com.swagritech.opsapp.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
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
                    AuthSession.idToken = tokenResponse.idToken
                    AuthSession.refreshToken = tokenResponse.refreshToken
                    AuthSession.expiresAtSeconds = tokenResponse.accessTokenExpirationTime?.div(1000L)
                    val claims = tokenResponse.idToken?.let { parseIdTokenClaims(it) }
                    AuthSession.username = claims?.name
                        ?: tokenResponse.additionalParameters["name"]
                        ?: tokenResponse.additionalParameters["preferred_username"]
                        ?: tokenResponse.additionalParameters["upn"]
                        ?: "Microsoft User"
                    AuthSession.principalName = claims?.preferredUsername
                        ?: tokenResponse.additionalParameters["preferred_username"]
                        ?: tokenResponse.additionalParameters["upn"]
                        ?: AuthSession.username
                    AuthSession.principalId = claims?.objectId

                    // Exchange provider token for EasyAuth token used by Azure App Service auth pipeline.
                    val (easyAuth, easyAuthError) = EasyAuthBridge.exchangeForSession(
                        accessToken = tokenResponse.accessToken,
                        idToken = tokenResponse.idToken
                    )
                    AuthSession.easyAuthToken = easyAuth
                    val hasSessionCookie = ApiClient.hasSessionCookies()

                    if (easyAuth.isNullOrBlank() && !hasSessionCookie) {
                        AuthSession.lastError = easyAuthError
                            ?: "Signed in, but EasyAuth session token was not returned"
                    } else {
                        AuthSession.lastError = null
                    }

                    AuthStore.save(
                        this@AuthCallbackActivity,
                        PersistedAuth(
                            accessToken = AuthSession.accessToken ?: "",
                            idToken = AuthSession.idToken,
                            refreshToken = AuthSession.refreshToken,
                            username = AuthSession.username,
                            expiresAtSeconds = AuthSession.expiresAtSeconds,
                            easyAuthToken = AuthSession.easyAuthToken,
                            principalName = AuthSession.principalName,
                            principalId = AuthSession.principalId
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

    private data class IdTokenClaims(
        val name: String?,
        val preferredUsername: String?,
        val objectId: String?
    )

    private fun parseIdTokenClaims(idToken: String): IdTokenClaims? {
        return runCatching {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val json = JSONObject(payload)
            val name = json.optString("name").ifBlank { null }
            val preferred = json.optString("preferred_username")
                .ifBlank { json.optString("upn") }
                .ifBlank { json.optString("email") }
                .ifBlank { null }
            val oid = json.optString("oid").ifBlank { null }
            IdTokenClaims(name = name, preferredUsername = preferred, objectId = oid)
        }.getOrNull()
    }

}
