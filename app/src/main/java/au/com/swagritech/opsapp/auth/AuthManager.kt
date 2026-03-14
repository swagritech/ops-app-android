package au.com.swagritech.opsapp.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import au.com.swagritech.opsapp.api.ApiClient
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AuthManager(private val context: Context) {
    private val serviceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse(AuthConfig.authorizationEndpoint),
        Uri.parse(AuthConfig.tokenEndpoint)
    )

    fun startSignIn(activity: Activity) {
        val request = AuthorizationRequest.Builder(
            serviceConfiguration,
            AuthConfig.clientId,
            "code",
            Uri.parse(AuthConfig.redirectUri)
        )
            .setScope(AuthConfig.scopes)
            .build()

        val authService = AuthorizationService(activity)
        val completionIntent = Intent(activity, AuthCallbackActivity::class.java)
        val cancelIntent = Intent(activity, AuthCallbackActivity::class.java).apply {
            putExtra("auth_cancelled", true)
        }

        authService.performAuthorizationRequest(
            request,
            PendingIntent.getActivity(
                activity,
                1001,
                completionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            ),
            PendingIntent.getActivity(
                activity,
                1002,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )
    }

    fun restoreFromStore() {
        val saved = AuthStore.load(context) ?: return
        AuthSession.accessToken = saved.accessToken
        AuthSession.idToken = saved.idToken
        AuthSession.refreshToken = saved.refreshToken
        AuthSession.username = saved.username
        AuthSession.expiresAtSeconds = saved.expiresAtSeconds
        AuthSession.easyAuthToken = saved.easyAuthToken
        AuthSession.principalName = saved.principalName
        AuthSession.principalId = saved.principalId
    }

    suspend fun refreshIfNeeded(): Result<Boolean> {
        if (AuthSession.isAccessTokenLikelyValid()) return Result.success(false)

        val refreshToken = AuthSession.refreshToken
        if (refreshToken.isNullOrBlank()) return Result.success(false)

        val authService = AuthorizationService(context)
        val request = TokenRequest.Builder(serviceConfiguration, AuthConfig.clientId)
            .setGrantType("refresh_token")
            .setRefreshToken(refreshToken)
            .setScope(AuthConfig.scopes)
            .build()

        return suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(request) { tokenResponse, tokenEx ->
                if (tokenResponse != null && !tokenResponse.accessToken.isNullOrBlank()) {
                    AuthSession.accessToken = tokenResponse.accessToken
                    AuthSession.idToken = tokenResponse.idToken ?: AuthSession.idToken
                    AuthSession.refreshToken = tokenResponse.refreshToken ?: refreshToken
                    AuthSession.expiresAtSeconds = tokenResponse.accessTokenExpirationTime?.div(1000L)

                    AuthStore.save(
                        context,
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
                    cont.resume(Result.success(true))
                } else {
                    AuthSession.lastError = tokenEx?.message ?: tokenEx?.errorDescription ?: "Refresh failed"
                    cont.resume(Result.failure(IllegalStateException(AuthSession.lastError ?: "Refresh failed")))
                }
            }
        }
    }

    suspend fun ensureApiSession(): Result<Boolean> {
        if (!AuthSession.easyAuthToken.isNullOrBlank() || ApiClient.hasSessionCookies()) {
            return Result.success(true)
        }

        val accessToken = AuthSession.accessToken
        if (accessToken.isNullOrBlank()) {
            AuthSession.lastError = "Session expired. Sign in with Microsoft again."
            return Result.success(false)
        }

        val (easyAuthToken, easyAuthError) = withContext(Dispatchers.IO) {
            EasyAuthBridge.exchangeForSession(
                accessToken = accessToken,
                idToken = AuthSession.idToken
            )
        }
        AuthSession.easyAuthToken = easyAuthToken
        val sessionOk = !easyAuthToken.isNullOrBlank() || ApiClient.hasSessionCookies()

        if (sessionOk) {
            AuthStore.save(
                context,
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
            return Result.success(true)
        }

        AuthSession.lastError = easyAuthError ?: "Unable to establish API auth session"
        return Result.failure(IllegalStateException(AuthSession.lastError ?: "Unable to establish API auth session"))
    }

    fun signOut() {
        AuthSession.clear()
        AuthStore.clear(context)
        ApiClient.clearSessionCookies()
    }
}
