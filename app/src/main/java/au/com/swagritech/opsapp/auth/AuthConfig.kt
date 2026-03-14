package au.com.swagritech.opsapp.auth

import au.com.swagritech.opsapp.BuildConfig

object AuthConfig {
    const val clientId = "e2e5195e-35d6-4141-9f78-cf3342bcc353"
    const val tenantId = "6b34f3c5-799e-43fb-a02f-31b8e7126489"
    const val redirectUri = "swatops://auth/callback"

    val authorizationEndpoint =
        "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/authorize"
    val tokenEndpoint =
        "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token"

    val scopes: String = BuildConfig.AUTH_SCOPE
}
