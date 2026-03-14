package au.com.swagritech.opsapp.auth

object AuthSession {
    @Volatile
    var accessToken: String? = null

    @Volatile
    var username: String? = null

    @Volatile
    var lastError: String? = null

    fun clear() {
        accessToken = null
        username = null
        lastError = null
    }
}
