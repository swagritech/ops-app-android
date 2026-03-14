package au.com.swagritech.opsapp.auth

import android.content.Context

data class PersistedAuth(
    val accessToken: String,
    val refreshToken: String?,
    val username: String?,
    val expiresAtSeconds: Long?,
    val easyAuthToken: String?,
    val principalName: String?,
    val principalId: String?
)

object AuthStore {
    private const val PREF = "swat_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_EASYAUTH_TOKEN = "easyauth_token"
    private const val KEY_PRINCIPAL_NAME = "principal_name"
    private const val KEY_PRINCIPAL_ID = "principal_id"

    fun save(context: Context, auth: PersistedAuth) {
        val editor = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
        editor.putString(KEY_ACCESS_TOKEN, auth.accessToken)
        editor.putString(KEY_REFRESH_TOKEN, auth.refreshToken)
        editor.putString(KEY_USERNAME, auth.username)
        editor.putString(KEY_EASYAUTH_TOKEN, auth.easyAuthToken)
        editor.putString(KEY_PRINCIPAL_NAME, auth.principalName)
        editor.putString(KEY_PRINCIPAL_ID, auth.principalId)
        if (auth.expiresAtSeconds != null) {
            editor.putLong(KEY_EXPIRES_AT, auth.expiresAtSeconds)
        } else {
            editor.remove(KEY_EXPIRES_AT)
        }
        editor.apply()
    }

    fun load(context: Context): PersistedAuth? {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        val username = prefs.getString(KEY_USERNAME, null)
        val easyAuthToken = prefs.getString(KEY_EASYAUTH_TOKEN, null)
        val principalName = prefs.getString(KEY_PRINCIPAL_NAME, null)
        val principalId = prefs.getString(KEY_PRINCIPAL_ID, null)
        val expiresAt = if (prefs.contains(KEY_EXPIRES_AT)) prefs.getLong(KEY_EXPIRES_AT, 0L) else null
        return PersistedAuth(access, refresh, username, expiresAt, easyAuthToken, principalName, principalId)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
