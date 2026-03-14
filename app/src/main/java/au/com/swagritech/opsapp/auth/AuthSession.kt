package au.com.swagritech.opsapp.auth

object AuthSession {
    @Volatile
    var accessToken: String? = null

    @Volatile
    var username: String? = null

    @Volatile
    var lastError: String? = null

    @Volatile
    var refreshToken: String? = null

    @Volatile
    var expiresAtSeconds: Long? = null

    @Volatile
    var easyAuthToken: String? = null

    @Volatile
    var principalName: String? = null

    @Volatile
    var principalId: String? = null

    fun clear() {
        accessToken = null
        username = null
        lastError = null
        refreshToken = null
        expiresAtSeconds = null
        easyAuthToken = null
        principalName = null
        principalId = null
    }

    fun isAccessTokenLikelyValid(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        val token = accessToken
        val exp = expiresAtSeconds
        if (token.isNullOrBlank()) return false
        if (exp == null) return true
        return (exp - nowEpochSeconds) > 60
    }
}
