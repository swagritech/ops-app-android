package au.com.swagritech.opsapp.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration

class AuthManager(private val activity: Activity) {
    private val serviceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse(AuthConfig.authorizationEndpoint),
        Uri.parse(AuthConfig.tokenEndpoint)
    )

    fun startSignIn() {
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
            android.app.PendingIntent.getActivity(
                activity,
                1001,
                completionIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            ),
            android.app.PendingIntent.getActivity(
                activity,
                1002,
                cancelIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
