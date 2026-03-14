package au.com.swagritech.opsapp.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import au.com.swagritech.opsapp.MainActivity
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
                AuthSession.username = tokenResponse.idToken ?: "Microsoft User"
                AuthSession.lastError = null
            } else {
                AuthSession.clear()
                AuthSession.lastError = tokenEx?.errorDescription ?: tokenEx?.message ?: "Token exchange failed"
            }
            returnToApp()
        }
    }

    private fun returnToApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }
}
