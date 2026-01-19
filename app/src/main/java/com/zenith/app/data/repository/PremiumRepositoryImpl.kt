package com.zenith.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.model.SubscriptionType
import com.zenith.app.domain.repository.PremiumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PremiumRepository {

    companion object {
        private val KEY_SUBSCRIPTION_TYPE = stringPreferencesKey("subscription_type")
        private val KEY_EXPIRES_AT = stringPreferencesKey("expires_at")
        private val KEY_TRIAL_STARTED_AT = stringPreferencesKey("trial_started_at")
        private val KEY_TRIAL_EXPIRES_AT = stringPreferencesKey("trial_expires_at")
        private val KEY_IS_TRIAL_USED = booleanPreferencesKey("is_trial_used")
        private val KEY_HAS_SEEN_TRIAL_OFFER = booleanPreferencesKey("has_seen_trial_offer")

        private const val TRIAL_DAYS = 7L
    }

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val subscriptionStatus: Flow<SubscriptionStatus> = dataStore.data.map { prefs ->
        parseSubscriptionStatus(prefs)
    }

    override suspend fun getSubscriptionStatus(): SubscriptionStatus {
        return subscriptionStatus.first()
    }

    override suspend fun startTrial() {
        dataStore.edit { prefs ->
            if (prefs[KEY_IS_TRIAL_USED] != true) {
                val now = LocalDateTime.now()
                val expires = now.plusDays(TRIAL_DAYS)
                prefs[KEY_TRIAL_STARTED_AT] = now.format(formatter)
                prefs[KEY_TRIAL_EXPIRES_AT] = expires.format(formatter)
                prefs[KEY_IS_TRIAL_USED] = true
                prefs[KEY_HAS_SEEN_TRIAL_OFFER] = true
            }
        }
    }

    override suspend fun updateSubscription(type: SubscriptionType, expiresAt: LocalDateTime?) {
        dataStore.edit { prefs ->
            prefs[KEY_SUBSCRIPTION_TYPE] = type.name
            if (expiresAt != null) {
                prefs[KEY_EXPIRES_AT] = expiresAt.format(formatter)
            } else {
                prefs.remove(KEY_EXPIRES_AT)
            }
        }
    }

    override suspend fun canAccessFeature(feature: PremiumFeature): Boolean {
        val status = getSubscriptionStatus()
        return status.isPremium
    }

    override suspend fun markTrialOfferSeen() {
        dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_TRIAL_OFFER] = true
        }
    }

    override suspend fun clearSubscription() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SUBSCRIPTION_TYPE)
            prefs.remove(KEY_EXPIRES_AT)
            prefs.remove(KEY_TRIAL_STARTED_AT)
            prefs.remove(KEY_TRIAL_EXPIRES_AT)
            prefs.remove(KEY_IS_TRIAL_USED)
            prefs.remove(KEY_HAS_SEEN_TRIAL_OFFER)
        }
    }

    private fun parseSubscriptionStatus(prefs: Preferences): SubscriptionStatus {
        val typeString = prefs[KEY_SUBSCRIPTION_TYPE]
        val type = typeString?.let {
            try {
                SubscriptionType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("PremiumRepository", "Unknown subscription type: $it, defaulting to FREE")
                SubscriptionType.FREE
            }
        } ?: SubscriptionType.FREE

        return SubscriptionStatus(
            type = type,
            expiresAt = prefs[KEY_EXPIRES_AT]?.let { parseDateTime(it) },
            trialStartedAt = prefs[KEY_TRIAL_STARTED_AT]?.let { parseDateTime(it) },
            trialExpiresAt = prefs[KEY_TRIAL_EXPIRES_AT]?.let { parseDateTime(it) },
            isTrialUsed = prefs[KEY_IS_TRIAL_USED] ?: false,
            hasSeenTrialOffer = prefs[KEY_HAS_SEEN_TRIAL_OFFER] ?: false
        )
    }

    private fun parseDateTime(str: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(str, formatter)
        } catch (e: Exception) {
            android.util.Log.w("PremiumRepository", "Failed to parse datetime: $str", e)
            null
        }
    }
}
