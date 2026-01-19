package com.zenith.app.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationPrefsDataStore by preferencesDataStore(
    name = "notification_permission_prefs"
)

class NotificationPermissionPrefs(private val context: Context) {

    companion object {
        private val KEY_PERMISSION_REQUESTED = booleanPreferencesKey("permission_requested")
        private val KEY_PERMISSION_DENIED_PERMANENTLY = booleanPreferencesKey("permission_denied_permanently")
    }

    val hasRequestedPermission: Flow<Boolean> = context.notificationPrefsDataStore.data
        .map { prefs -> prefs[KEY_PERMISSION_REQUESTED] ?: false }

    val isPermissionDeniedPermanently: Flow<Boolean> = context.notificationPrefsDataStore.data
        .map { prefs -> prefs[KEY_PERMISSION_DENIED_PERMANENTLY] ?: false }

    suspend fun setPermissionRequested(requested: Boolean) {
        context.notificationPrefsDataStore.edit { prefs ->
            prefs[KEY_PERMISSION_REQUESTED] = requested
        }
    }

    suspend fun setPermissionDeniedPermanently(denied: Boolean) {
        context.notificationPrefsDataStore.edit { prefs ->
            prefs[KEY_PERMISSION_DENIED_PERMANENTLY] = denied
        }
    }
}
