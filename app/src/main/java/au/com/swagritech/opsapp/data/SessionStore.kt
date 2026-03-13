package au.com.swagritech.opsapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "swat_session")

class SessionStore(private val context: Context) {
    private val pilotKey = stringPreferencesKey("currentPilot")

    val currentPilot: Flow<String?> = context.dataStore.data.map { prefs: Preferences ->
        prefs[pilotKey]
    }

    suspend fun setCurrentPilot(name: String) {
        context.dataStore.edit { prefs ->
            prefs[pilotKey] = name
        }
    }
}
