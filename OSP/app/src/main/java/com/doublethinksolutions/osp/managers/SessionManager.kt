package com.doublethinksolutions.osp.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Top-level extension property to create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "osp_session")

class SessionManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        // Define keys for the values we want to store
        val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val KEY_GOOGLE_ID_TOKEN = stringPreferencesKey("google_id_token")
        val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    // --- Read operations return a Flow ---

    /**
     * A flow that emits the access token whenever it changes, or null if it's not set.
     */
    val accessTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_ACCESS_TOKEN]
    }

    val refreshTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_REFRESH_TOKEN]
    }

    /**
     * A flow that emits true if the user has completed the onboarding flow, otherwise false.
     */
    val hasCompletedOnboardingFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAS_COMPLETED_ONBOARDING] ?: false
    }

    // --- Write operations are suspend functions ---


    suspend fun saveAccessToken(accessToken: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
        }
    }

    /**
     * Saves the backend access and refresh tokens.
     */
    suspend fun saveBackendTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
            preferences[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    /**
     * Saves the Google ID token.
     */
    suspend fun saveGoogleIdToken(idToken: String) {
        dataStore.edit { preferences ->
            preferences[KEY_GOOGLE_ID_TOKEN] = idToken
        }
    }

    /**
     * Persists the user's onboarding completion status.
     * @param completed true if the user has finished onboarding, false otherwise.
     */
    suspend fun saveOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    /**
     * Clears all stored session data.
     */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
