package com.zenith.app.domain.repository

import com.zenith.app.domain.model.PremiumFeature
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.model.SubscriptionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface PremiumRepository {
    val subscriptionStatus: Flow<SubscriptionStatus>

    suspend fun getSubscriptionStatus(): SubscriptionStatus
    suspend fun startTrial()
    suspend fun updateSubscription(type: SubscriptionType, expiresAt: LocalDateTime?)
    suspend fun canAccessFeature(feature: PremiumFeature): Boolean
    suspend fun markTrialOfferSeen()
    suspend fun clearSubscription() // デバッグ用
}
