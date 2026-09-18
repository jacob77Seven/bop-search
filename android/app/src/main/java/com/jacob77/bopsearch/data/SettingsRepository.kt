package com.jacob77.bopsearch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class PeerSettings(
    val host: String = "powerspec",
    val port: Int = 8765,
    val clientId: String = "android",
)

class SettingsRepository(private val context: Context) {
    private val hostKey = stringPreferencesKey("peer_host")
    private val portKey = intPreferencesKey("peer_port")
    private val clientIdKey = stringPreferencesKey("client_id")

    val settings: Flow<PeerSettings> = context.dataStore.data.map { prefs ->
        PeerSettings(
            host = prefs[hostKey] ?: "powerspec",
            port = prefs[portKey] ?: 8765,
            clientId = prefs[clientIdKey] ?: "android",
        )
    }

    suspend fun update(host: String, port: Int, clientId: String = "android") {
        context.dataStore.edit { prefs ->
            prefs[hostKey] = host.trim().ifEmpty { "powerspec" }
            prefs[portKey] = port.coerceIn(1, 65535)
            prefs[clientIdKey] = clientId.trim().ifEmpty { "android" }
        }
    }
}
